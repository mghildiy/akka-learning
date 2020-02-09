package com.cypherlabs.akka.playground.scala.iot

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import scala.concurrent.duration._

object DeviceGroup {

  // factory method for creating a DeviceGroup for a given group id
  def apply(groupId: String): Behavior[Command] =
    Behaviors.setup[DeviceGroup.Command](context => new DeviceGroup(context, groupId))

  trait Command
  private final case class DeviceTerminated(device: ActorRef[Device.Command], groupId: String, deviceId: String)
    extends Command

}

class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String) extends AbstractBehavior[DeviceGroup.Command](context) {
  context.log.info(s"Device group with id $groupId started")

  import DeviceGroup._
  import DeviceManager._

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  override def onMessage(msg: DeviceGroup.Command): Behavior[DeviceGroup.Command] = {
    msg match {
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) => replyTo ! DeviceRegistered(deviceActor)
          case None =>
            context.log.info(s"Creating device actor for $deviceId under group $groupId")
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
            // death watch the device actor as supervision strategy doesn't cover graceful stopping
            context.watchWith(deviceActor, DeviceTerminated(deviceActor, groupId, deviceId))
            deviceIdToActor += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      case RequestTrackDevice(grpId, _, _) =>
        context.log.warn(s"This device group is responsible for $groupId. Hence ignoring track device request for $grpId.")
        this

      case RequestDeviceList(requestId, grpId, replyTo) =>
        if(grpId == groupId) {
          replyTo ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
          this
        }
        else
          // reuse the previous behaviour
          Behaviors.unhandled

      case DeviceTerminated(deviceActor, groupId, deviceId) =>
        context.log.info(s"Device $deviceId under group $groupId terminated")
        deviceIdToActor -= deviceId
        this

      case RequestAllTemperatures(requestId, gId, replyTo) =>
        if (gId == groupId) {
          context.spawnAnonymous(
            DeviceGroupQuery(deviceIdToActor, requestId = requestId, requester = replyTo, 3.seconds))
          this
        } else
          Behaviors.unhandled
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[DeviceGroup.Command]] = {
    case PostStop =>
      context.log.info(s"Stopping device group $groupId")
      this
  }
}


