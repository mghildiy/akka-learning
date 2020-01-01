package com.cypherlabs.akka.testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.cypherlabs.akka.testing.BasicSpec.{BlackHole, LabTestActor, SimpleActor}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
{
  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  "Simple actor" should {
    "send back the same message" in {
      val simpleActor = system.actorOf(Props[SimpleActor])
      val message = "Hello, test"
      simpleActor ! message

      expectMsg(message)
    }
  }

  "BlackHole actor" should {
    "not send back any message" in {
      val blakHoleActor = system.actorOf(Props[BlackHole])
      val message = "Hello, test"
      blakHoleActor ! message

      expectNoMessage(1 second)
    }
  }

  "LabTest actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])
    "convert string to upper case" in {
      labTestActor ! "I feel small, make me big"

      val reply = expectMsgType[String]
      assert(reply == "I FEEL SMALL, MAKE ME BIG")
    }

    "reply to greeting with hi or hello" in {
      labTestActor ! "greeting"

      expectMsgAnyOf("hi", "hello")
    }

    "reply to favourite tech with Scala and Akka" in {
      labTestActor ! "favourite tech"

      expectMsgAllOf("Scala", "Akka")
    }

    "reply to favourite tech with Scala and Akka in another way" in {
      labTestActor ! "favourite tech"

      expectMsgPF() {
        case "Scala" =>
        case "Akka" =>
      }
    }
  }
}

object BasicSpec{
  class SimpleActor extends Actor {
    override def receive(): Receive  = {
      case message => sender() ! message
    }
  }

  class BlackHole extends Actor {
    override def receive(): Receive  = {
      case _  => // do nothing
    }
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" => if(random.nextBoolean()) sender ! "hi" else sender ! "hello"
      case "favourite tech" =>
        sender ! "Scala"
        sender ! "Akka"
      case message: String => sender ! message.toUpperCase()
    }
  }
}
