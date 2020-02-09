package com.cypherlabs.akka.faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, Terminated}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SuspervisionSpec extends TestKit(ActorSystem("SuspervisionSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
{

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  import SuspervisionSpec._

  "Supervisor actor" should {
    "Resume child actor in case of minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      // send message which would make child actor throw RuntimeException
      child ! "I love Spring as it helps fast development of services"
      child ! Report
      // as actor is resumed, its state is unchanged
      expectMsg(3)
    }

    "Restart child actor in case of empty string" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      // send message which would make child actor throw NullPointerException
      child ! " "
      child ! Report
      // as actor is restarted, actor instance is replaced
      expectMsg(0)
    }

    "terminate its child in case of major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! "i love akka"
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }

    "escalate an error when it doesnt know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! 5
      // escalate would kill the child actor
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }

  "A kinder supervisor" should {
    "not kill children in case of escalation or restart" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "kindersupervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! 45
      child ! Report
      expectMsg(0)
    }
  }

}

object SuspervisionSpec {
  class Supervisor extends Actor {
    override val supervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {

    }
  }

  case object Report
  class FussyWordCounter extends Actor {
    var words = 0
    override def receive: Receive = {
      case sentence: String =>
        if(sentence.trim.equals("")) throw new NullPointerException("sentence is empty")
        if(sentence.length > 20) throw new RuntimeException("sentence is too long")
        else if(!Character.isUpperCase(sentence.charAt(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        else words += sentence.split(" ").length
      case Report => sender ! words
      case _ => throw new Exception("can only receive strings")
    }
  }
}
