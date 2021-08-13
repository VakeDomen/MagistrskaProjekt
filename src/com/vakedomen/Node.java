package com.vakedomen;

import com.vakedomen.events.DeactivateNodeEvent;
import com.vakedomen.events.MessageReceivedEvent;
import com.vakedomen.events.MessageSentEvent;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Node {
    private String id;
    private ArrayList<Node> network;
    private boolean informed;
    private boolean active;
    private int simCount;

    public Node(String node_id, int simCount) {
        this.id = node_id;
        this.informed = false;
        this.active = true;
        this.simCount = simCount;
    }

    public String getId() {
        return id;
    }

    public void setNetwork(ArrayList<Node> network) {
        this.network = network;
    }

    public void digestMessage(Node sender) {
        if (!this.informed) {
            this.informed = true;
            Main.informedCount++;
        }
        if (this.active && simCount == Main.simCount) {
            Main.checkEndPropagation();
            Main.broadcastMessage(new MessageReceivedEvent(sender, this));
            sendMessage();
        }
    }

    public void sendMessage() {
        if (this.simCount != Main.simCount) {
            return;
        }
        Node randomReciever;
        while ((randomReciever = network.get((int)(Math.random() * network.size()))) == this) {}
        Main.broadcastMessage(new MessageSentEvent(this, randomReciever));
        final Node sender = this;
        final Node reciever = randomReciever;
        Runnable task = () -> reciever.digestMessage(sender);
        Main.executor.schedule(task, Util.calcLatencyBetweenNodes(this, reciever), TimeUnit.MILLISECONDS);
    }

    public void deactivate() {
        this.active = false;
        Main.broadcastMessage(new DeactivateNodeEvent(this));
    }
}
