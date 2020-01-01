package com.cypherlabs.akka.scalabasics

object Basics extends App{
  val partialFunction: PartialFunction[Int, Int] = {
    case 1=> 42
    case 2 => 65
    case 5 => 999
  }

  println(partialFunction(1))
}
