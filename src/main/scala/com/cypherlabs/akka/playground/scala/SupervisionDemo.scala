package com.cypherlabs.akka.playground.scala

import akka.actor.typed.{ActorSystem, Behavior, PostStop, PreRestart, Signal, SupervisorStrategy}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object SupervisionDemo extends App {

  val testSystem = ActorSystem(Supervisor(), "testSystem")
  testSystem ! "failChild"

}

object Supervisor {

  def apply() = Behaviors.setup(context => new Supervisor(context))

}

class Supervisor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("supervisor starts")
  val child = context.spawn(
    Behaviors.supervise(Supervised()).onFailure(SupervisorStrategy.restart),
    name = "supervised-actor")

  override def onMessage(msg: String) =
    msg match {
      case "failChild" =>
        child ! "fail"
        this
    }

}

object Supervised {

  def apply() = Behaviors.setup(context => new Supervised(context))

}

class Supervised(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("supervised starts")

  override def onMessage(msg: String) = msg match {
    case "fail" =>
      println("Supervised failing")
      throw new Exception("Supervised failed")
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PreRestart =>
      println("Supervised restarting")
      this
    case PostStop =>
      println("Supservised stopped")
      this
  }
}
