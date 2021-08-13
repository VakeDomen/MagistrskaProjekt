package com.vakedomen.events;

import com.vakedomen.Node;

public class DeactivateNodeEvent extends Event {
    private String nodeId;
        public DeactivateNodeEvent(Node node) {
        super("deactivate_node");
        this.nodeId = node.getId();
    }
}
