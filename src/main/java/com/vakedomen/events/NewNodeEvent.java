package com.vakedomen.events;

import com.vakedomen.core.Event;
import com.vakedomen.core.Node;

public class NewNodeEvent extends Event {

    private String node_id;

    public NewNodeEvent(String nodeId) {
        super("new_node");
        this.node_id = nodeId;
    }

    public NewNodeEvent(Node node) {
        super("new_node");
        this.node_id = node.getId();
    }

    public String getNodeId() {
        return node_id;
    }

}
