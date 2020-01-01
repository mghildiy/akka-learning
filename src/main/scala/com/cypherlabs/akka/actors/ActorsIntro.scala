package com.cypherlabs.akka.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.cypherlabs.akka.actors.ActorsIntro.BankAccount.{Deposit, Statement, TransactionFailure, TransactionSuccess, WithDraw}
import com.cypherlabs.akka.actors.ActorsIntro.Counter.{Decrement, Increment, Print}
import com.cypherlabs.akka.actors.ActorsIntro.Customer.{ManageMyMoney}

object ActorsIntro extends App{
  val actorSystem = ActorSystem("FirstActorSystem")
  println(actorSystem.name)

  class WordCountActor extends Actor {
    var totalWords = 0

    def receive: PartialFunction[Any, Unit] = {
      case msg:String =>
        println(s"[word counter]I have received message: ${msg}")
        totalWords += msg.split(" ").length
      case msg => println(s"[word counter] I don't understand: ${msg.toString()}")
    }
  }

  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")
  wordCounter ! "I am learning akka"
  anotherWordCounter ! "Another message"

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    var count = 0

    def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"[counter] My current count is $count")
    }
  }

  val counter = actorSystem.actorOf(Props[Counter], "counter")
  counter ! Increment
  counter ! Print

  object BankAccount {
    case class Deposit(amount: Double)
    case class WithDraw(amount: Double)
    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
    case object Statement
  }

  class BankAccount extends Actor {
    var funds = 0.0

    def receive: Receive = {
      case Deposit(amount) =>
        if(amount < 0) sender() ! TransactionFailure("Invalid deposit amount")
        else {
          this.funds += amount
          sender() ! TransactionSuccess(s"Successfully deposited amount $amount")
        }

      case WithDraw(amount) =>
        if(amount < 0) sender() ! TransactionFailure("Invalid withdraw amount")
        else if(this.funds < amount) sender() ! TransactionFailure("Account doesn't have enough funds")
        else {
          this.funds -= amount
          sender() ! TransactionSuccess(s"Successfully withdrew amount $amount")
        }

      case Statement => sender() ! s"Current funds are $funds"
    }
  }

  object Customer {
    case class ManageMyMoney(account: ActorRef)
  }

  class Customer extends Actor {
    def receive: Receive = {
      case ManageMyMoney(actorRef) =>
        actorRef ! Deposit(1000)
        actorRef ! WithDraw(1500)
        actorRef ! WithDraw(500)
        actorRef ! Statement
      case message => println(message)
    }
  }

  val bankAccount = actorSystem.actorOf(Props[BankAccount], "bankAccount")
  val customer = actorSystem.actorOf(Props[Customer], "customer")
  customer ! ManageMyMoney(bankAccount)
}
