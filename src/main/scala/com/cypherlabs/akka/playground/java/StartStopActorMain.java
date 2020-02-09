package com.cypherlabs.akka.playground.java;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class StartStopActorMain {

    public static void main(String[] args) {
        ActorRef<String> testSystem = ActorSystem.create(StartStopActor1.create(), "testSystem");
        testSystem.tell("stop");
    }

}

class StartStopActor1 extends AbstractBehavior<String> {

    // factory method ot create this actor
    static public Behavior<String> create() {
        return Behaviors.setup(StartStopActor1::new);
    }

    private StartStopActor1(ActorContext<String> context) {
        super(context);
        System.out.println("First actor started");
        context.spawn(StartStopActor2.create(), "second-actor");
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals("stop", Behaviors::stopped)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<String> onPostStop() {
        System.out.println("first actor stopped");
        return this;
    }
}

class StartStopActor2 extends AbstractBehavior<String> {

    // factory method to create this actor
    static Behavior<String> create() {
        return Behaviors.setup(StartStopActor2::new);
    }

    private StartStopActor2(ActorContext<String> context) {
        super(context);
        System.out.println("Second actor started");
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private Behavior<String> onPostStop() {
        System.out.println("second actor stopped");
        return this;
    }
}


