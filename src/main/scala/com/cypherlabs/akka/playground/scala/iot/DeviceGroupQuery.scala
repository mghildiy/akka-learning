package com.cypherlabs.akka.playground.scala.iot

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.TimerScheduler
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import scala.concurrent.duration.FiniteDuration

object DeviceGroupQuery {

  def apply(
             deviceIdToActor: Map[String, ActorRef[Device.Command]],
             requestId: Long,
             requester: ActorRef[DeviceManager.RespondAllTemperatures],
             timeout: FiniteDuration) =
    Behaviors.setup[Command](context => {
      Behaviors.withTimers(timers => new DeviceGroupQuery(
        deviceIdToActor, requestId, requester, timeout, context, timers
      ))
    })

  trait Command;
  private case object CollectionTimeOut extends Command
  final case class WrappedRespondTemperature(response: Device.RespondTemperature) extends Command
  private final case class DeviceTerminated(deviceId: String) extends Command
}

class DeviceGroupQuery(
           deviceIdToActor: Map[String, ActorRef[Device.Command]],
           requestId: Long,
           requester: ActorRef[DeviceManager.RespondAllTemperatures],
           timeout: FiniteDuration,
           context: ActorContext[DeviceGroupQuery.Command],
           timers: TimerScheduler[DeviceGroupQuery.Command])
  extends AbstractBehavior[DeviceGroupQuery.Command](context) {
  import DeviceGroupQuery._
  import DeviceManager._

  timers.startSingleTimer(CollectionTimeOut, CollectionTimeOut, timeout)
  private val respondTemperatureAdapter = context.messageAdapter(WrappedRespondTemperature.apply)

  deviceIdToActor.foreach {
    case (deviceId, device) => {
      context.watchWith(device, DeviceTerminated(deviceId))
      device ! Device.ReadTemperature(0, respondTemperatureAdapter)
    }
  }

  private var repliesSoFar = Map.empty[String, TemperatureReading]
  private var stillWaiting = deviceIdToActor.keySet

  override def onMessage(msg: DeviceGroupQuery.Command): Behavior[DeviceGroupQuery.Command] =
    msg match {
      case WrappedRespondTemperature(response) => onRespondTemperature(response)
      case DeviceTerminated(deviceId)          => onDeviceTerminated(deviceId)
      case DeviceGroupQuery.CollectionTimeOut  => onCollectionTimout()
    }

  private def onRespondTemperature(response: Device.RespondTemperature): Behavior[DeviceGroupQuery.Command] = {
    val reading = response.value match {
      case Some(value) => Temperature(value)
      case None        => TemperatureNotAvailable
    }

    val deviceId = response.deviceId
    repliesSoFar += (deviceId -> reading)
    stillWaiting -= deviceId
    respondWhenAllCollected()
  }

  private def onDeviceTerminated(deviceId: String): Behavior[DeviceGroupQuery.Command] = {
    if (stillWaiting(deviceId)) {
      repliesSoFar += (deviceId -> DeviceNotAvailable)
      stillWaiting -= deviceId
    }
    respondWhenAllCollected()
  }

  private def onCollectionTimout(): Behavior[DeviceGroupQuery.Command] = {
    repliesSoFar ++= stillWaiting.map(deviceId => deviceId -> DeviceTimedOut)
    stillWaiting = Set.empty
    respondWhenAllCollected()
  }

  private def respondWhenAllCollected(): Behavior[DeviceGroupQuery.Command] = {
    if(stillWaiting.isEmpty) {
      requester ! DeviceManager.RespondAllTemperatures(requestId, repliesSoFar)
      Behaviors.stopped
    }else {
      this
    }
  }

}
