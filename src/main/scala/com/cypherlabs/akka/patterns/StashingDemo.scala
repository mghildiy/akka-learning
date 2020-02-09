package com.cypherlabs.akka.patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashingDemo extends App{

  case object Open
  case object Closed
  case object Read
  case class Write(data: String)

  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I am currently closed")
        stash()
    }

    def open: Receive =  {
      case Read =>
        log.info(s"Reading $innerData")
      case Write(data) =>
        log.info(s"Writing $data")
        innerData = data
      case Closed =>
        log.info("Closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I cn't handle it in open state")
        stash()
    }
  }

  val system = ActorSystem("ResourceActor")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Write("Stash is awesome")
  resourceActor ! Read
  resourceActor ! Open
}
