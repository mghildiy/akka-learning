package com.cypherlabs.akka.playground.scala.iot

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}

object DeviceManager {

  sealed trait Command
  final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered])
    extends Command with DeviceGroup.Command
  final case class DeviceRegistered(device: ActorRef[Device.Command])

  final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList])
    extends Command with DeviceGroup.Command
  final case class ReplyDeviceList(requestId: Long, deviceIds: Set[String])

  private final case class DeviceGroupTerminated(groupId: String) extends Command

  //#query-protocol
  final case class RequestAllTemperatures(requestId: Long, groupId: String, replyTo: ActorRef[RespondAllTemperatures])
    extends DeviceGroupQuery.Command
      with DeviceGroup.Command
      with DeviceManager.Command

  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  sealed trait TemperatureReading
  final case class Temperature(value: Double) extends TemperatureReading
  case object TemperatureNotAvailable extends TemperatureReading
  case object DeviceNotAvailable extends TemperatureReading
  case object DeviceTimedOut extends TemperatureReading
  //#query-protocol

}

class DeviceManager(context: ActorContext[DeviceManager.Command])
  extends AbstractBehavior[DeviceManager.Command](context) {
  context.log.info(s"Device manager started")
  import DeviceManager._

  private var groupIdToActor = Map.empty[String, ActorRef[DeviceGroup.Command]]

  override def onMessage(msg: DeviceManager.Command): Behavior[DeviceManager.Command] = {
    msg match {
      case trackMsg @ RequestTrackDevice(groupId, deviceId, replyTo) =>
        groupIdToActor.get(groupId) match {
          case Some(ref) =>
            ref ! trackMsg
          case None =>
            context.log.info(s"Creating device group $groupId")
            val deviceGroup = context.spawn(DeviceGroup(groupId), s"group-$groupId")
            context.watchWith(deviceGroup, DeviceGroupTerminated(groupId))
            deviceGroup ! trackMsg
            groupIdToActor += groupId -> deviceGroup
        }
        this

      case req @ RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList]) =>
        groupIdToActor.get(groupId) match {
          case Some(ref) =>
            ref ! req
          case None =>
            replyTo ! ReplyDeviceList(requestId, Set.empty)
        }
        this


      case DeviceGroupTerminated(groupId) =>
        context.log.info(s"Group $groupId terminated")
        groupIdToActor -= groupId
        this
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[DeviceManager.Command]] = {
    case PostStop =>
      context.log.info(s"Stopping device manager")
      this
  }

}
