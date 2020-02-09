package com.cypherlabs.akka.playground.scala

import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}


object StartStopActorMain extends App {

  val first = ActorSystem(StartStopActor1(), "firstActor")
  println(first)
  first ! "stop"

}

object StartStopActor1 {

  def apply() =
    Behaviors.setup(context => new StartStopActor1(context))

}

class StartStopActor1(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("first started")
  context.spawn(StartStopActor2(), "second")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "stop" => Behaviors.stopped
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("first stopped")
      this
  }
}

object StartStopActor2 {

  def apply() =
    Behaviors.setup(context => new StartStopActor2(context))

}

class StartStopActor2(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("second started")

  override def onMessage(msg: String): Behavior[String] = Behaviors.unhandled

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("second stopped")
      this
  }
}
