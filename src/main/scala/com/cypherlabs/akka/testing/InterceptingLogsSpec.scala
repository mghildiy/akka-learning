package com.cypherlabs.akka.testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class InterceptingLogsSpec extends TestKit(
  ActorSystem("InterceptingLogsSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
{

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

}

object InterceptingLogsSpec {
  case class Checkout(item: String, card: String)
  case class AuthorizeCard(card: String)
  case object PaymentAccepted
  case object PaymentDenied
  case class DispatchOrder(item: String)
  case object OrderConfirmed

  class CheckoutActor extends Actor {
    val paymentManager = context.actorOf(Props[PaymentManager])
    val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, card) =>
        this.paymentManager ! AuthorizeCard(card)
        context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        this.fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment(item))
      case PaymentDenied => ???
    }

    def pendingFulfillment(item: String): Receive = {
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizeCard(card) =>
        if(card.startsWith("0")) sender ! PaymentDenied
        else sender ! PaymentAccepted
    }
  }

  class FulfillmentManager extends Actor with ActorLogging{
    var orderId = 0
    override def receive: Receive = {
      case DispatchOrder(item) =>
        orderId += 1
        log.info(s"Order with $orderId for item $item has been dispatched")
        sender ! OrderConfirmed
    }
  }
}
