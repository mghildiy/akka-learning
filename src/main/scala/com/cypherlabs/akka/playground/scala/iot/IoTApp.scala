package com.cypherlabs.akka.playground.scala.iot

import akka.actor.typed.ActorSystem


object IoTApp extends App {

  ActorSystem[Nothing](IoTSupervisor(), "iot-application")

}
