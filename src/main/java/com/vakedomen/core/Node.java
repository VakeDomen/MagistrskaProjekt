package com.vakedomen.core;

import com.vakedomen.Main;
import com.vakedomen.config.SimulationConfig;
import com.vakedomen.events.*;
import com.vakedomen.helpers.Triple;
import com.vakedomen.helpers.Util;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.vakedomen.config.Flood.FLOOD_FAN_OUT;
import static com.vakedomen.config.Tree.ACK_WAIT_TIME;

public class Node {
    private SimulationConfig conf;

    private String id;
    // used in TREE algorithm
    private ArrayList<Node> network;
    // used in FLOOD algorithm
    private ArrayList<Node> neighbours;

    private boolean informed;
    private boolean active;
    private int simCount;
    private ArrayList<Message> ackQue = new ArrayList<>();
    private ArrayList<String> messageHashes = new ArrayList<>();

    public Node(String node_id, int simCount, SimulationConfig conf) {
        this.id = node_id;
        this.informed = false;
        this.active = true;
        this.simCount = simCount;
        this.neighbours = new ArrayList<>();
        this.conf = conf;
    }

    public String getId() {
        return id;
    }

    public void setNetwork(ArrayList<Node> network) {
        this.network = (ArrayList<Node>) network.clone();
    }

    public void digestMessage(Message message, Node sender, boolean requireAck) {
        switch (this.conf.getAlgo()){
            case TREE:
                digestTree(message, sender, requireAck);
                break;
            case FLOOD:
                digestFlood(message, sender);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.conf.getAlgo());
        }
    }

    private void digestFlood(Message message, Node sender) {
        inform(message, sender);

        if (!this.active || simCount != Main.simCount) {
            return;
        }

        if (!messageHashes.contains(message.getId())) {
            messageHashes.add(message.getId());
            List<Node> receivers = getReceiversFlood();
            for (Node n : receivers) {
                Message m = new Message(message.getId(), message.getGenerator(), message.getPath() + "0", Message.Type.DATA, message.getHop() + 1);
                sendMessage(n, m, false);
            }
        }

    }

    private void digestTree(Message message, Node sender, boolean requireAck) {
        inform(message, sender);



        // SEND ACK IF REQUIRED
        if (requireAck && active) {
            Message ack = new Message(this, "", Message.Type.ACK, 1);
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
        System.out.println("rec " + receivers.get(0) + " " + receivers.get(1) + " " + receivers.get(2));
        Triple<Node, Node, String> recipients = Util.calculateRecipients(receivers, path);

        if (recipients.getFirst() != null) {
            System.out.println("[" + this.id.substring(0, 5) + "] Informing fist child (" + recipients.getFirst().getId().substring(0,5) + ")");
            Message message0 = new Message(message.getId(), message.getGenerator(), path + "0", Message.Type.DATA, message.getHop() + 1);
            sendMessage(recipients.getFirst(), message0, false);
        } else {
            System.out.println("[" + this.id.substring(0, 5) + "] No first child!");
        }
        if (recipients.getSecond() != null) {
            System.out.println("[" + this.id.substring(0, 5) + "] Informing second child (" + recipients.getSecond().getId().substring(0,5) + ")");
            Message message1 = new Message(message.getId(), message.getGenerator(), path + "1", Message.Type.DATA, message.getHop() + 1);
            sendMessage(recipients.getSecond(), message1, false);
        }else {
            System.out.println("[" + this.id.substring(0, 5) + "] No second child!");
        }

        if (recipients.getThird() != null) {
            Node neighbour = receivers.get(Util.findNodeIndexFromPath(recipients.getThird()) - 2);
            System.out.println("[" + this.id.substring(0, 5) + "] Informing neighbour (" + neighbour.getId().substring(0,5) + ") PATH: " + path + " -> " + recipients.getThird());
            System.out.println("");
            Message message2 = new Message(message.getId(), message.getGenerator(), recipients.getThird(), Message.Type.DATA, message.getHop() + 1);
            sendMessage(neighbour, message2, true);
            Runnable task = () -> checkAck(neighbour, recipients.getThird(), message2);
            Main.executor.schedule(task, ACK_WAIT_TIME, TimeUnit.MILLISECONDS);
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

    private void inform(Message message, Node sender) {
        if (this.active) {
            Main.broadcastMessage(new MessageReceivedEvent(sender, this));
        } else {
            Main.broadcastMessage(new FailedToSendMessage(sender, this));
        }
        if (!this.informed) {
            this.informed = true;
            Main.informedCount++;
            Main.hops.add(message.getHop());
            if (message.getHop() > Main.maxHop) {
                Main.maxHop = message.getHop();
            }
        }
        Main.checkEndPropagation(this.conf.getNetworkSize());
    }

    public void sendMessage(Node receiver, final Message message, final boolean requireAck) {
        if (this.simCount != Main.simCount && message.getType() == Message.Type.DATA) {
            //System.out.println("------------ OLD SIMULATION MESSAGE! NOT SENT! -----------");
            //return;
        }
        final Node sender = this;
        final boolean ack = requireAck;
        if (message.getType() != Message.Type.ACK) {
            Main.broadcastMessage(new MessageSentEvent(this, receiver));
        }
        Runnable task = () -> receiver.digestMessage(message, sender, ack);
        Main.executor.schedule(task, Util.calcLatencyBetweenNodes(sender, receiver), TimeUnit.MILLISECONDS);
        Main.sentMessages++;
    }

    public void deactivate() {
        if(this.active) {
            this.active = false;
            Main.brokenNodes++;
        }
    }

    public void generateMessage() {
        switch (this.conf.getAlgo()){
            case TREE:
                generateMessageTree();
                break;
            case FLOOD:
                generateMessageFlood();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.conf.getAlgo());
        }

    }

    private void generateMessageFlood() {

        Main.broadcastMessage(new MessageGeneratedEvent(this));
        String messageHash = Util.hash("" + System.currentTimeMillis());
        List<Node> receivers = getReceiversFlood();
        inform(
                new Message(messageHash,this, "", Message.Type.DATA,0),
                this
        );
        for (Node n : receivers) {
            Message message = new Message(messageHash, this, "0", Message.Type.DATA, 1);
            sendMessage(n, message, false);
        }
    }

    private List<Node> getReceiversFlood() {
        int index = new Random().nextInt(neighbours.size());
        if (index <= neighbours.size() - FLOOD_FAN_OUT) {
            return neighbours.subList(index, index + FLOOD_FAN_OUT);
        }
        int diff = index - (neighbours.size() - FLOOD_FAN_OUT);
        List<Node> out = neighbours.subList(index, neighbours.size());
        out.addAll(neighbours.subList(0, diff));
        return out;
    }

    private void generateMessageTree() {

        Main.broadcastMessage(new MessageGeneratedEvent(this));
        String messageHash = Util.hash("" + System.currentTimeMillis());
        Message message0 = new Message(messageHash, this, "0", Message.Type.DATA, 1);
        Message message1 = new Message(messageHash, this, "1", Message.Type.DATA, 1);
        ArrayList<Node> nodes = Util.pseudoRandomMix(
                Util.generateInitialReceiverList(network, this),
                Util.hashToSeed(message1.getId())
        );
        // Child1, Child2, Neighbour to check
        Triple<Node, Node, String > recipients = Util.calculateRecipients(nodes, "");
        sendMessage(recipients.getFirst(), message0, false);
        sendMessage(recipients.getSecond(), message1, false);
        inform(
            new Message(messageHash, this, "", Message.Type.DATA, 0),
                this
        );
    }

    public boolean isInformed() {
        return informed;
    }

    public void addNeighbours(ArrayList<Node> neigh) {
        for (Node n : neigh) {
            neighbours.add(n);
        }
    }
    public boolean addNeighbour(Node neigh) {
        if (neighbours.contains(neigh)) {
            return false;
        }
        neighbours.add(neigh);
        return true;
    }
    public ArrayList<Node> getNeighbours() {
        return neighbours;
    }

    public boolean isActive() {
        return this.active;
    }
}
