package com.cypherlabs.akka.playground.scala.iot

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object Device {

  def apply(groupId: String, deviceId: String) =
    Behaviors.setup[Command](context => new Device(context, groupId, deviceId))

  sealed trait Command
  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(requestId: Long, deviceId: String, value: Option[Double])

  // #write-protocol
  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded]) extends Command
  final case class TemperatureRecorded(requestId: Long)
  // #write-protocol

  case object Passivate extends Command

}

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String)
  extends AbstractBehavior[Device.Command](context) {
  import Device._

  var lastTemperatureReading: Option[Double] = None
  context.log.info(s"Device actor $groupId-$deviceId started")

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case ReadTemperature(requestId, replyTo) =>
        replyTo ! RespondTemperature(requestId, deviceId, lastTemperatureReading)
        this

      case RecordTemperature(requestId, value, replyTo) =>
        context.log.info(s"Device actor $groupId-$deviceId recorded $value with id $requestId from sender $replyTo")
        lastTemperatureReading = Option.apply(value)
        replyTo ! TemperatureRecorded(requestId)
        this

      case Passivate =>
        // signal to the system that this actor should terminate voluntarily(not error scenario)
        Behaviors.stopped
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info(s"Device actor $groupId-$deviceId stopped")
      this
  }

}
