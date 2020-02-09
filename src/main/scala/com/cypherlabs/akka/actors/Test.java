package com.cypherlabs.akka.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Test {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("my-system");
        ActorRef myActor
                = system.actorOf(Props.create(MyActor.class), "my-actor");

        MyActor.Message message = new MyActor.Message("Hello Lombok!");
        myActor.tell(message, ActorRef.noSender());
    }
}
