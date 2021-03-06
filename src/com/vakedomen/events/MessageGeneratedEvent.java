package com.vakedomen.events;

import com.vakedomen.Node;

public class MessageGeneratedEvent extends Event {
    private String nodeId;

    public MessageGeneratedEvent(String nodeId) {
        super("new_message_generated");
        this.nodeId = nodeId;
    }

    public MessageGeneratedEvent(Node node) {
        super("new_message_generated");
        this.nodeId = node.getId();
    }
}
