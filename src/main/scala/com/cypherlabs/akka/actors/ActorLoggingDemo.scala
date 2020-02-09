package com.cypherlabs.akka.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object ActorLoggingDemo extends App{

  class ActorWithExplicitLogger extends Actor {
    //val logger = Logging(context.system, this)
    override def receive: Receive = {
      case message => //logger.info(message.toString)
    }
  }

  val actorSystem = ActorSystem("LoggingDemo")
  println(actorSystem)
  val actor = actorSystem.actorOf(Props[ActorWithExplicitLogger])
  println(actor)
  actor ! "Logging a simple message"

  class ActorWithInbuiltLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case message: String => log.info(message)
      case (a, b) => println(s"Two inputs are: $a and $b") //log.info("Two inputs are: {} and {}", a, b)
    }
  }

  val actorWithInbuiltLogging = actorSystem.actorOf(Props[ActorWithInbuiltLogging])
  actorWithInbuiltLogging ! "Logging a simple message by extending a trait"
  actorWithInbuiltLogging ! (2, 5)
}
