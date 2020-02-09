package com.cypherlabs.akka.patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "Authenticator" should {
    import AuthManager._

    "fail to authenticate unregistered user" in {
      val authManager = system.actorOf(Props[AuthManager])

      authManager ! Authenticate("dummyUN", "dummyPW")

      expectMsg(AuthFailure(USERNAME_NOT_FOUND))
    }

    "fail to authenticate user if password is wrong" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("dummyUN", "dummyPW")

      authManager ! Authenticate("dummyUN", "dummyP")

      expectMsg(AuthFailure(WRONG_PASSWORD))
    }
  }
}

object AskSpec {
  case class Read(key: String)
  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Reading for key $key")
        sender ! kv.get(key)
      case Write(key, value) =>
        log.info(s"Writing value $value for key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  case class RegisterUser(userName: String, passWord: String)
  case class Authenticate(userName: String, passWord: String)
  case class AuthFailure(message: String)
  case object AuthSuccess

  object AuthManager {
    val USERNAME_NOT_FOUND = "username not found"
    val WRONG_PASSWORD = "Wrong password"
    val SYSTEM_FAILURE = "system failure"
  }

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._

    implicit val timeOut: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher
    private val authDB = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(userName, passWord) =>
        authDB ! Write(userName, passWord)
      case Authenticate(userName, passWord) =>
        handleAuthentication(userName, passWord)
    }

    def handleAuthentication(userName: String, passWord: String) = {
      val originalSender = sender
      val future = authDB ? Read(userName)
      future.onComplete {
        case Success(None) => originalSender ! AuthFailure(USERNAME_NOT_FOUND)
        case Success(dbPassword) =>
          if(dbPassword == passWord) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(WRONG_PASSWORD)
        case Failure(_) => originalSender ! AuthFailure(SYSTEM_FAILURE)
      }
    }
  }

}
