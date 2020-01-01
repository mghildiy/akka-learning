package com.cypherlabs.akka.playground

import akka.actor.{ActorSystem}

object PlayGround extends App {
  val actorSystem = ActorSystem("HelloAkka")
  println(actorSystem.name)
}
