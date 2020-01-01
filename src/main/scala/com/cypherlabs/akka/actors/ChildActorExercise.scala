package com.cypherlabs.akka.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.cypherlabs.akka.actors.ChildActorExercise.WordCounterMaster.{Initialize, WordCountTask}


object ChildActorExercise extends App{

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(taskId: Int, text: String)
    case class WordCountReply(taskId: Int, count: Int)
  }

  class WordCounterMaster extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren) => {
        println(s"[wordcountmaster] asked to create $nChildren workers")
        var childActorRefs: List[ActorRef] = List()
        (0 to nChildren-1).foreach(i => {
          childActorRefs = context.actorOf(Props[WordCounterWorker], s"wcw_$i") :: childActorRefs
        })
        context.become(withWorkers(childActorRefs, 0, 0, Map()))
      }
    }
    def withWorkers(workers: List[ActorRef], currentWorkerIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String => {
        println(s"[wordcountmaster] asked to count words in :$text. I will send it to $currentWorkerIndex.")
        val originalSender = sender()
        workers(currentWorkerIndex) ! WordCountTask(currentTaskId, text)
        val nextWorkerIndex = (currentWorkerIndex + 1) % workers.length
        val newTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withWorkers(workers, nextWorkerIndex, newTaskId, newRequestMap))
      }
      case WordCountReply(id, count) => {
        println(s"[wordcountermaster] I have received reply for task id: $id with $count.")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withWorkers(workers, currentWorkerIndex, currentTaskId, requestMap - id))
      }
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(id, text) => {
        println(s"${self.path} I have been asked to count words in taskId: $id for text: $text")
        sender ! WordCountReply(id, text.split("\\s+").length)
      }
    }
  }

  class TestActor extends Actor {
    override def receive: Receive = {
      case "go" => {
        val master = context.actorOf(Props[WordCounterMaster],"wordCounterMaster")
        master ! Initialize(5)
        val texts = List(
          "How many words?",
          "How many words now?",
          "How many words, in this case?",
          "How many words here?",
          "How many words in it?",
          "Tell me how many words?")
        texts.foreach(text => master ! text)
      }
      case count =>
        println(s"[testactor] I received reply $count.")
    }
  }

  val actorSystem = ActorSystem("childActorExercise")
  val testActor = actorSystem.actorOf(Props[TestActor], "testActor")
  testActor ! "go"

  /*master ! Initialize(5)
  master ! "How many words?"
  master ! "How many words now?"
  master ! "How many words, in this case?"
  master ! "How many words here?"
  master ! "How many words in it?"
  master ! "Tell me how many words?"*/

}
