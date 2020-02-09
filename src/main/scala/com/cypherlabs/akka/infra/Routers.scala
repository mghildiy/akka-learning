package com.cypherlabs.akka.infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RandomRoutingLogic, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Routers extends App{

  // #1 Manual router
  class Master extends Actor {
    // 5 actor routees based on slaves
    val slaves = for(i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    private var router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {
      case Terminated(ref) =>
        router = router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)
      case message =>
        router.route(message, sender)
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info("Received "+ message.toString)
    }
  }

  //val system = ActorSystem("RoutersDemo")

  /*val master = system.actorOf(Props[Master])
  for(i <- 1 to 10) {
    master ! s"[$i] Hello World"
  }*/

  // #2 Router actor with its own children: Pool Router
  // 2.1: From code
  //val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
  /*for(i <- 1 to 10) {
    poolMaster ! s"[$i] Hello World"
  }*/
  // 2.2: From config
  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  for(i <- 1 to 10) {
    poolMaster2 ! s"[$i] Hello World"
  }

  // #3 Router with actors created somewhere else in application: Group master

}
