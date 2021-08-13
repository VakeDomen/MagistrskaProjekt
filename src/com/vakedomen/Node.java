package com.vakedomen;

import com.sun.tools.jconsole.JConsoleContext;
import com.sun.tools.jconsole.JConsolePlugin;
import com.vakedomen.events.*;
import org.jgrapht.alg.util.Triple;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Node {
    private String id;
    private ArrayList<Node> network;
    private boolean informed;
    private boolean active;
    private int simCount;
    private ArrayList<Message> ackQue = new ArrayList<>();
    private ArrayList<String> messageHashes = new ArrayList<>();

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

    public void digestMessage(Message message, Node sender, boolean requireAck) {
        inform();

        if (this.active) {
            Main.broadcastMessage(new MessageReceivedEvent(sender, this));
        } else {
            Main.broadcastMessage(new FailedToSendMessage(sender, this));
        }

        // SEND ACK IF REQUIRED
        if (requireAck && active) {
            Message ack = new Message(this, "", Message.Type.ACK);
            System.out.println("[" + this.id.substring(0, 5) + "] Sending ack to " + sender.getId().substring(0,5));
            sendMessage(sender, ack, false);
        }
        // HANDLE RECEIVED ACK
        if (message.getType() == Message.Type.ACK) {
            ackQue.add(message);
            System.out.println("[" + this.id.substring(0, 5) + "] Received ack from " + sender.getId().substring(0,5));
            return;
        }

        // should not propagate message
        if (!this.active || simCount != Main.simCount) {
            return;
        }
        if (!messageHashes.contains(message.getId())) {
            messageHashes.add(message.getId());
            if (requireAck) {
                System.out.println("[" + this.id.substring(0, 5) + "] MESSAGE FROM NEIGHBOUR FIRST " + sender.getId().substring(0,5));
            }
            informNodesAsNodeFromPath(message, message.getPath());
        }
    }

    private void informNodesAsNodeFromPath(Message message, String path) {
        ArrayList<Node> receivers = Util.pseudoRandomMix(
                Util.generateInitialReceiverList(network, message.getGenerator()),
                Util.hashToSeed(message.getId())
        );
        Triple<Node, Node, String > recipients = Util.calculateRecipients(receivers, path);

        if (recipients.getFirst() != null) {
            System.out.println("[" + this.id.substring(0, 5) + "] Informing fist child (" + recipients.getFirst().getId().substring(0,5) + ")");
            Message message0 = new Message(message.getId(), message.getGenerator(), message.getPath() + "0", Message.Type.DATA);
            sendMessage(recipients.getFirst(), message0, false);
        } else {
            System.out.println("[" + this.id.substring(0, 5) + "] No first child!");
        }
        if (recipients.getSecond() != null) {
            System.out.println("[" + this.id.substring(0, 5) + "] Informing second child (" + recipients.getSecond().getId().substring(0,5) + ")");
            Message message1 = new Message(message.getId(), message.getGenerator(), message.getPath() + "1", Message.Type.DATA);
            sendMessage(recipients.getSecond(), message1, false);
        }else {
            System.out.println("[" + this.id.substring(0, 5) + "] No second child!");
        }

        if (recipients.getThird() != null) {
            Node neighbour = receivers.get(Util.findNodeIndexFromPath(recipients.getThird()) - 2);
            System.out.println("[" + this.id.substring(0, 5) + "] Informing neighbour (" + neighbour.getId().substring(0,5) + ") PATH: " + path + " -> " + recipients.getThird());
            System.out.println("");
            Message message2 = new Message(message.getId(), message.getGenerator(), recipients.getThird(), Message.Type.DATA);
            sendMessage(neighbour, message2, true);
            Runnable task = () -> checkAck(neighbour, recipients.getThird(), message);
            Main.executor.schedule(task, Main.ACK_WAIT_TIME + 100, TimeUnit.MILLISECONDS);
        }


    }

    private void checkAck(Node neighbour, String neighboursPath, Message originalMessage) {
        System.out.println("[" + this.id.substring(0, 5) + "] Checking neighbour ACK paket (" + neighbour.getId().substring(0,5) + ") NEIGH-PATH: " + neighboursPath);

        // if we received ack, we remove it from the que in return
        for (Message message : ackQue) {
            if (message.getGenerator() == neighbour) {
                ackQue.remove(message);
                System.out.println("[" + this.id.substring(0, 5) + "] ACK Acquired! (" + neighbour.getId().substring(0,5) + ") NEIGH-PATH: " + neighboursPath);
                return;
            }
        }
        System.out.println("[" + this.id.substring(0, 5) + "] NO ACK PACKET!! (" + neighbour.getId().substring(0,5) + ") NEIGH-PATH: " + neighboursPath + " | INFORMING CHILDREN!");
        // else in the name of our neighbour inform his children
        informNodesAsNodeFromPath(originalMessage, neighboursPath);
    }

    private void inform() {
        if (!this.informed) {
            this.informed = true;
            Main.informedCount++;
        }
        Main.checkEndPropagation();
    }

    public void sendMessage(Node receiver, final Message message, final boolean requireAck) {
        if (this.simCount != Main.simCount && message.getType() == Message.Type.DATA) {
            System.out.println("------------ OLD SIMULATION MESSAGE! NOT SENT! -----------");
            //return;
        }
        final Node sender = this;
        final boolean ack = requireAck;
        if (message.getType() != Message.Type.ACK) {
            Main.broadcastMessage(new MessageSentEvent(this, receiver));
        }
        Runnable task = () -> receiver.digestMessage(message, sender, ack);
        Main.executor.schedule(task, Util.calcLatencyBetweenNodes(sender, receiver), TimeUnit.MILLISECONDS);
    }

    public void deactivate() {
        if(this.active) {
            this.active = false;
        }
    }

    public void generateMessage() {
        inform();
        Main.broadcastMessage(new MessageGeneratedEvent(this));
        String messageHash = Util.hash("" + System.currentTimeMillis());
        Message message0 = new Message(messageHash, this, "0", Message.Type.DATA);
        Message message1 = new Message(messageHash, this, "1", Message.Type.DATA);
        ArrayList<Node> nodes = Util.pseudoRandomMix(
                Util.generateInitialReceiverList(network, this),
                Util.hashToSeed(message1.getId())
        );
        // Child1, Child2, Neighbour to check
        Triple<Node, Node, String > recipients = Util.calculateRecipients(nodes, "");
        sendMessage(recipients.getFirst(), message0, false);
        sendMessage(recipients.getSecond(), message1, false);

        System.out.println(nodes.stream().map((Node n) -> n.getId().substring(0, 5)).collect(Collectors.toList()));
    }

    public boolean isInformed() {
        return informed;
    }

}
