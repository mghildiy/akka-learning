package com.cypherlabs.akka.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsDemo extends App{

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  class Parent extends Actor {
    import Parent._
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child" )
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => childRef forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I received: $message" )
    }
  }

  import Parent._

  val actorSystem = ActorSystem("ParentChileDemo")
  val parent = actorSystem.actorOf(Props[Parent],"parent")
  parent ! CreateChild("child")
  parent ! TellChild("Hello Kid!")

  /*
  // Guardian actors
  - / -> root guardian actor(manages /user and /system guardian actors)
  - /user -> user level guardian actor(manages all actors created by developers)
  - /system -> system guardian ctor(manages actors related to various akka services like logging )
  */

  // Actor Selection - finding an actor by path
  val childSelection = actorSystem.actorSelection("/user/parent/chil")
  childSelection ! "I found you"
}
