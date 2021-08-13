package com.vakedomen;

import com.sun.tools.jconsole.JConsoleContext;
import com.sun.tools.jconsole.JConsolePlugin;
import com.vakedomen.events.DeactivateNodeEvent;
import com.vakedomen.events.MessageGeneratedEvent;
import com.vakedomen.events.MessageReceivedEvent;
import com.vakedomen.events.MessageSentEvent;
import org.jgrapht.alg.util.Triple;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        this.network = (ArrayList<Node>) network.clone();
    }

    public void digestMessage(Message message, Node sender) {
        if (this.active) {
            //Main.broadcastMessage(new MessageReceivedEvent(sender, this));
        }
        inform();
        // should not propagate message
        if (!this.active || simCount != Main.simCount) {
            return;
        }

        ArrayList<Node> receivers = Util.pseudoRandomMix(
                Util.generateInitialReceiverList(network, message.getGenerator()),
                Util.hashToSeed(message.getId())
        );

        Triple<Node, Node, Node > recipients = Util.calculateRecipients(receivers, message.getPath());
        if (recipients.getFirst() != null) {
            System.out.println("[" + this.id.substring(0, 5) + "] Informing fist child (" + recipients.getFirst().getId().substring(0,5) + ")");
            Message message0 = new Message(message.getId(), message.getGenerator(), message.getPath() + "0");
            sendMessage(network.get(network.indexOf(recipients.getFirst())), message0);
        }
        if (recipients.getSecond() != null) {
            System.out.println("[" + this.id.substring(0, 5) + "] Informing second child (" + recipients.getSecond().getId().substring(0,5) + ")");
            Message message1 = new Message(message.getId(), message.getGenerator(), message.getPath() + "1");
            sendMessage(network.get(network.indexOf(recipients.getSecond())), message1 );
        }
    }

    private void inform() {
        if (!this.informed) {
            this.informed = true;
            Main.informedCount++;
        }
        Main.checkEndPropagation();
    }

    public void sendMessage(Node target, Message payload) {
        if (this.simCount != Main.simCount) {
            return;
        }
        final Node sender = this;
        final Node receiver = target;
        final Message message = payload;

        Main.broadcastMessage(new MessageSentEvent(this, receiver));
        Runnable task = () -> receiver.digestMessage(message, sender);
        Main.executor.schedule(task, Util.calcLatencyBetweenNodes(sender, receiver), TimeUnit.MILLISECONDS);
    }

    public void deactivate() {
        this.active = false;
        Main.broadcastMessage(new DeactivateNodeEvent(this));
    }

    public void generateMessage() {
        inform();
        Main.broadcastMessage(new MessageGeneratedEvent(this));
        String messageHash = Util.hash("" + System.currentTimeMillis());
        Message message0 = new Message(messageHash, this, "0");
        Message message1 = new Message(messageHash, this, "1");
        ArrayList<Node> nodes = Util.pseudoRandomMix(
                Util.generateInitialReceiverList(network, this),
                Util.hashToSeed(message1.getId())
        );
        // Child1, Child2, Neighbour to check
        Triple<Node, Node, Node > recipients = Util.calculateRecipients(nodes, "");
        sendMessage(network.get(network.indexOf(recipients.getFirst())), message0);
        sendMessage(network.get(network.indexOf(recipients.getSecond())), message1);
    }

    public boolean isInformed() {
        return informed;
    }

}
