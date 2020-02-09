package com.cypherlabs.akka.playground.scala.iot

import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

class IoTSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context){
  println("IoT application started")

  override def onMessage(msg: Nothing) = {
    // we don't handle any messages for '/user' actor
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      context.log.info("IoT application stopped")
      this
  }

}

object IoTSupervisor {

  def apply() = Behaviors.setup[Nothing](context => new IoTSupervisor(context))

}
