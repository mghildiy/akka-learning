package com.cypherlabs.akka.testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class TimedAssertionsSpec extends TestKit(
  ActorSystem("TimedAssertionsSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
{

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  import TimedAssertionsSpec._

  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])

    "reply in a timely manner" in {
      within(500 millis, 1 second) {
        workerActor ! "work"

        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work in reasonable timeframe" in
      {
      workerActor ! "worksequence"

      val results:Seq[Int] = receiveWhile[Int](max= 2 seconds, idle= 500 millis, messages= 10) {
        case WorkResult(result) => result
      }

      assert(results.sum > 5)
    }

    "reply to test probe in timely manner" in {
      within(1 second) {
        val probe = TestProbe()
        // probe has its own timeout of 0.3 sec from specialTimedAssertionsConfig.akka.test.single-expect-default
        // and is not influenced by within's 1 second. Hence would fail
        probe.send(workerActor, "work")
        probe.expectMsg(WorkResult(42))
      }
    }
  }

}

object TimedAssertionsSpec {
case class WorkResult(result: Int)

  class WorkerActor extends Actor {
    override def receive = {
      case "work" =>
        // long computation
        Thread.sleep(500)
        sender ! WorkResult(42)
      case "worksequence" =>
        val r = new Random()
        for(i <- 1 to 10) {
          Thread.sleep(r.nextInt(50))
          sender ! WorkResult(1)
        }
    }
  }

}
