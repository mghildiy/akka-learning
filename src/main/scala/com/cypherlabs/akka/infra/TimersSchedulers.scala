package com.cypherlabs.akka.infra

import akka.actor.AbstractActor.Receive
import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}

import scala.concurrent.duration._

object TimersSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("TimersSchedulersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  /*system.log.info("Scheduling reminder for simpleactor")
  system.scheduler.scheduleOnce(2 second){
    system.log.info("Sending reminder to simpleactor")
    simpleActor ! "reminder"
  }(system.dispatcher)

  system.log.info("Scheduling routine for simpleactor")
  val routine: Cancellable = system.scheduler.schedule(2 second, 2 second) {
    system.log.info("Sending heartbeat to simpleactor")
    simpleActor ! "heartbeat"
  }(system.dispatcher)

  system.scheduler.scheduleOnce(10 second){
    routine.cancel()
  }(system.dispatcher)*/

  class SelfClosingActor extends Actor with ActorLogging{
    var schedule: Cancellable = createTimeOutWindow

    def createTimeOutWindow: Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }(context.system.dispatcher)
    }

    override def receive: Receive = {
      case "timeout" =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        schedule.cancel
        log.info(s"Received message $message")
        schedule = createTimeOutWindow
    }
  }

  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
  system.scheduler.scheduleOnce(250 millis){
    selfClosingActor ! "ping"
  }(system.dispatcher)

  system.scheduler.scheduleOnce(2 seconds){
    system.log.info("Sending pong to selfclosingactor")
    selfClosingActor ! "pong"
  }(system.dispatcher)

  // Timers
}
