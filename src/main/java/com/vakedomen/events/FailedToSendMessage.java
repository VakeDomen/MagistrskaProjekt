package com.vakedomen.events;

import com.vakedomen.core.Node;

public class FailedToSendMessage extends Event {
    private String source;
    private String destination;

    public FailedToSendMessage(String source, String destination) {
        super("message_not_sent");
        this.source = source;
        this.destination = destination;
    }

    public FailedToSendMessage(Node source, Node destination) {
        super("message_not_sent");
        this.source = source.getId();
        this.destination = destination.getId();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
