package com.vakedomen;

import org.jgrapht.alg.util.Triple;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.Collectors;

public class Util {
    private static Random r = new Random();

    public static String generateNodeId(int i){
        return hash("prefix" + i);
    }

    public static String hash(String s) {
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(s);
    }

    public static long hashToSeed(String hash) {
        return Long.parseLong(hash.substring(0, 15).trim(), 16);
    }

    public static String mergeIdHash(Node n1, Node n2) {
        for (int i = 0 ; i < n1.getId().length() ; i++ ) {
            if (n1.getId().charAt(i) < n2.getId().charAt(i)) return n1.getId() + n2.getId();
            if (n1.getId().charAt(i) > n2.getId().charAt(i)) return n2.getId() + n1.getId();
        }
        return n1.getId() + n2.getId();
    }

    public static int calcLatencyBetweenNodes(Node n1, Node n2) {
        long seed = Util.hashToSeed(Util.hash(Util.mergeIdHash(n1, n2)));
        r.setSeed(seed);
        return r.nextInt(Main.MAXIMUM_LATENCY - Main.MINIMUM_LATENCY) + Main.MINIMUM_LATENCY;
    }


    public static ArrayList<Node> generateInitialReceiverList(ArrayList<Node> network, Node generator) {
        int genIndex = network.indexOf(generator);
        ArrayList<Node> rec = (ArrayList<Node>) network.clone();
        Node tmp = network.get(genIndex);
        network.remove(genIndex);
        rec.sort(Comparator.comparing(Node::getId));
        network.add(tmp);
        return rec;
    }

    public static ArrayList<Node> pseudoRandomMix(ArrayList<Node> ids, long seed) {
        r.setSeed(seed);
        for (int i = ids.size() - 1; i > 0; i--) {
            int index = r.nextInt(i + 1);
            // Simple swap
            Node tmp = ids.get(i);
            ids.set(i, ids.get(index));
            ids.set(index, tmp);
        }
        return ids;
    }

    public static Triple<Node, Node, Node> calculateRecipients(ArrayList<Node> receivers, String path) {
        //System.out.println("Path " + path);
        int nodeIndex = 1;
        for (int i = 0 ; i < path.length() ; i++) {
            if (path.charAt(i) == '0') {
                nodeIndex = nodeIndex * 2;
            } else {
                nodeIndex = nodeIndex * 2 + 1;
            }
            System.out.println(nodeIndex);
        }
        //System.out.println(receivers.stream().map((node -> node.getId())).collect(Collectors.toList()));
        //System.out.println("REC IND: " + ((nodeIndex*2)-1) + "[" +receivers.get((nodeIndex*2-1)).getId().substring(0, 5) + "] " + (nodeIndex*2));
        Node child1 = nodeIndex * 2 - 1 < receivers.size() ? receivers.get(nodeIndex * 2 - 1) : null;
        Node child2 = nodeIndex * 2     < receivers.size() ? receivers.get(nodeIndex * 2)     : null;
        return new Triple<>(child1, child2, child2);
    }
}
