package com.cypherlabs.akka.actors;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.Value;

public class MyActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public Receive createReceive() {
        return receiveBuilder().match(Message.class, message -> {
            System.out.println(message.getMessage());
        }).matchAny(o -> log.info("Received unknown message")).build();
    }

    @Value
    public static class Message {
        private String message;

        public Message(String message) {
            this.message = message;
        }
    }
}
