package com.cypherlabs.akka.faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingStoppingActors extends App{

  val system = ActorSystem("StoppingActorsDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"Stopping child $name")
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))
      case Stop =>
        log.info("Stopping myself")
        // first all children would be stopped recursively, then this actor would stop
        context.stop(self)
      case message =>
        log.info(message.toString)
    }
  }

  class Child extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  import Parent._

  // stopping actors using stop
  /*val parent = system.actorOf(Props[Parent],"parent")
  parent ! StartChild("child1")
  val child = system.actorSelection("user/parent/child1")
  child ! "Hi Kid!"
  parent ! StopChild("child1")
  // but as context.stop is asynchronous, so we may still get some messages handled by child actor
  for(_ <- 1 to 50) child ! "Are you still there?"

  parent ! StartChild("child2")
  val child2 = system.actorSelection("user/parent/child2")
  child2 ! "Hi another Kid!"
  // it would stop parent as well as children actors
  for(_ <- 1 to 4) parent ! "Are you there parent?"
  parent ! Stop
  // but as context.stop is asynchronous, so we may still get some messages handled by child actor
  for(_ <- 1 to 4) parent ! "Are you still there parent?"
  for(_ <- 1 to 5) child2 ! "Are you still there another kid?"*/

  // PoisonKill and Kill messages to kill actors. These messages can't be handled in normal receive message handler.
  // stopping an actor using PoisonPill
  /*val parentLessActor = system.actorOf(Props[Child])
  parentLessActor ! "Hello parent less actor!"
  parentLessActor ! PoisonPill
  parentLessActor ! "Hello parent less actor,you still there?"*/

  // stopping an actor using Kill. It throws exception unlike PoisonPill
  /*val anotherParentLessActor = system.actorOf(Props[Child])
  anotherParentLessActor ! "Hello another parent less actor!"
  anotherParentLessActor ! Kill
  anotherParentLessActor ! "Hello parent less actor,you still there?"*/

  // deathwatch
  class Watcher extends Actor with ActorLogging {
    override def receive: Receive= {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        // watch means this actor receives Terminated message when watched actor stops
        // Note: An actor can watch any actor, not necessarily only its child
        context.watch(child)
      // Note: Terminated is received even for those actors which are already dead when they are put under watch
      case Terminated(ref) =>
        log.info(s"Watched $ref has been stopped")
    }
  }

  val watcher = system.actorOf(Props[Watcher],"watcher")
  watcher ! StartChild("watchedChild")
  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500)
  watchedChild ! PoisonPill
}
