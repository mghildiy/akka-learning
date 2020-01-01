package com.cypherlabs.akka.testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
{

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  import TestProbeSpec._

  "A master actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave") // TestProbe is a mock actor whose behaviours we can mock

      master ! Register(slave.ref)

      expectMsg(RegistrationAck)
    }

    "send the work to the slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workLoadString = "I love Akka"
      master ! Work(workLoadString)

      slave.expectMsg(SlaveWork(workLoadString, testActor))

      slave.reply(WorkCompleted(3, testActor))

      expectMsg(Report(3))
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workLoadString1 = "I love Akka"
      val workLoadString2 = "I love Python too"
      master ! Work(workLoadString1)
      master ! Work(workLoadString2)

      slave.receiveWhile() {
        case SlaveWork(`workLoadString1`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
        case SlaveWork(`workLoadString2`, `testActor`) => slave.reply(WorkCompleted(4, testActor))
      }

      expectMsg(Report(3))
      expectMsg(Report(7))
    }
  }

}

object TestProbeSpec {

  case class Register(slaveRef: ActorRef)
  case object RegistrationAck
  case class Work(text: String)
  case class SlaveWork(text: String, originalRequester: ActorRef)
  case class WorkCompleted(count: Int, originalRequester: ActorRef)
  case class Report(totalCount: Int)

  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) =>
        sender ! RegistrationAck
        context.become(online(slaveRef, 0))
      case _ => // ignore
    }

    def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender)
      case WorkCompleted(count, originalRequester) =>
        val newWorldCount = totalWordCount + count
        originalRequester ! Report(newWorldCount)
        context.become(online(slaveRef, newWorldCount))
    }
  }



}
