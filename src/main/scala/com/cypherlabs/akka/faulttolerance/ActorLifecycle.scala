package com.cypherlabs.akka.faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifecycle extends App {

  object StartChild

  class LifecycleActor extends Actor with ActorLogging {
    override def preStart(): Unit = log.info(s"I, ${self}, am starting")
    override def postStop(): Unit = log.info(s"I, ${self} , have stopped")

    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }
  }

  val system = ActorSystem("ActorLifecycleDemo")

  /*val parent = system.actorOf(Props[LifecycleActor], "parent")
  parent ! StartChild
  parent ! PoisonPill*/

  case object FailChild
  case object CheckChild
  class Parent extends Actor with ActorLogging {
    private val child = system.actorOf(Props[Child],"supervisedChild")
    override def receive: Receive = {
      case FailChild =>
        child ! Fail
      case CheckChild =>
        child ! Check
    }
  }

  // restart
  // default supervision strategy: if an actor fails(throwing an exception) while processing a message, that message is removed from mailbox
  // and actor is restarted, and is ready to process next messages in mail box
  case object Fail
  case object Check
  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("supervised child started")
    override def postStop(): Unit = log.info("supervised child stopped")
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"supervised child restarting because of ${reason.getMessage}")
    }
    override def postRestart(reason: Throwable): Unit = {
      log.info("supervised child restarted")
    }

    override def receive: Receive = {
      case Fail =>
        log.info("Child will fail now")
        throw new RuntimeException("I failed")
      case Check =>
        log.info("alive and kicking")
    }
  }

  val parent = system.actorOf(Props[Parent], "supervisor")
  parent ! FailChild
  parent ! CheckChild

}
