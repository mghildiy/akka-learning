package com.cypherlabs.akka.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorsBehaviour extends App {

  object StateLessFussyKid {
    case object KidAccept
    case object KidReject
  }

  class StateLessFussyKid extends Actor {
    import StateLessFussyKid._
    import Mom._

    def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }

    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) =>  context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
  }

  object Mom {
    case class MomStart(kid: ActorRef)
    case class Food(food: String)
    case class Ask(message: String)
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  class Mom extends Actor {
    import Mom._
    import StateLessFussyKid._
    def receive: Receive = {
      case MomStart(kidRef) =>
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("Do you want to play?")
      case KidAccept => println("My kid is happy")
      case KidReject => println("My kid is sad, but healthy")
    }
  }

  val actorSystem = ActorSystem("changingActorBehaviourDemo")
  val stateLessFussyKid = actorSystem.actorOf(Props[StateLessFussyKid])
  val mom = actorSystem.actorOf(Props[Mom])
  import Mom._
  mom ! MomStart(stateLessFussyKid)

  println("#############################################")
  //stateless counter
  object StateLessCounter {
    case object Increment
    case object Decrement
    case object Print
  }

  class StateLessCounter extends Actor {
    import StateLessCounter._
    override def receive: Receive = countReceive(0)

    def countReceive(current: Int): Receive = {
      case Increment =>
        println(s"Incrementing $current")
        context.become(countReceive(current + 1))
      case Decrement =>
        println(s"Decrementing $current")
        context.become(countReceive(current - 1))
      case Print => println(s"Current value is $current")
    }
  }

  import com.cypherlabs.akka.actors.ChangingActorsBehaviour.StateLessCounter._
  val counter = actorSystem.actorOf(Props[StateLessCounter], "counter")
  counter ! Increment
  counter ! Decrement
  counter ! Print

  println("#############################################")
  // voting system
  /*class Citizen extends Actor {
    override def receive: Receive = {
    }
  }

  class VoteAggregator extends Actor {
    override def receive: Receive = {
    }
  }*/

}
