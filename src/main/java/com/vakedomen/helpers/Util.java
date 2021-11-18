package com.vakedomen.helpers;

import com.vakedomen.Main;
import com.vakedomen.core.Enums;
import com.vakedomen.core.Node;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import static com.vakedomen.config.Run.*;

public class Util {
    private static Random r = new Random();

    public static String generateNodeId(String i){
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
        return r.nextInt(MAXIMUM_LATENCY - MINIMUM_LATENCY) + MINIMUM_LATENCY;
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
            Node tmp = ids.get(i);
            ids.set(i, ids.get(index));
            ids.set(index, tmp);
        }
        return ids;
    }

    public static Triple<Node, Node, String> calculateRecipients(ArrayList<Node> receivers, String path) {
        int nodeIndex = findNodeIndexFromPath(path);
        Node child1 = nodeIndex * 2 - 2 < receivers.size() ? receivers.get(nodeIndex * 2 - 2) : null;
        Node child2 = nodeIndex * 2 - 1 < receivers.size() ? receivers.get(nodeIndex * 2 - 1) : null;
        String pathToNeighbour = retracePathOneNodeLeft(path);
        return new Triple<>(child1, child2, pathToNeighbour);
    }

    private static String retracePathOneNodeLeft(String path) {
        StringBuilder pathToNeighbour = new StringBuilder(path);
        for (int i = path.length() - 1 ; i >= 0 ; i--) {
            if (path.charAt(i) == '0') {
                pathToNeighbour.setCharAt(i, '1');
                break;
            }
            pathToNeighbour.setCharAt(i, '0');
        }
        return pathToNeighbour.toString();
    }

    public static int findNodeIndexFromPath(String path) {
        int nodeIndex = 1;
        for (int i = 0 ; i < path.length() ; i++) {
            if (path.charAt(i) == '0') nodeIndex = nodeIndex * 2;
            else nodeIndex = nodeIndex * 2 + 1;
        }
        return nodeIndex;
    }

    public static void log(String row) {
        if (!SAVE_DATA) {
            return;
        }
        try {
            FileWriter fw = new FileWriter(FILE_NAME, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            out.println(row);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> T randomElement(ArrayList<T> arrayList) {
        int index = r.nextInt(arrayList.size());
        return arrayList.get(index);
    }


    public static  void logArg(int simId, Enums.Algo algo, int n, int uninformed, float dbOdds, int dcCount, int nMsg, float avgHop, int maxHop, long totalTime, int fanout, int minLatency, int maxLatency, int ackWaitTime) {
        String[] args = {
                simId + "",
                algo + "",
                n + "",
                uninformed + "",
                dbOdds + "",
                dcCount + "",
                nMsg + "",
                avgHop + "",
                maxHop + "",
                totalTime + "",
                fanout + "",
                minLatency + "",
                maxLatency + "",
                ackWaitTime + ""
        };
        log(Arrays.stream(args).reduce("", (a, b) -> a + ";" + b).substring(1));
    }

    public static void createCsvFile() {
        try {
            File myObj = new File(FILE_NAME);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
                if (CLEAR_DATA) {
                    PrintWriter writer = new PrintWriter(myObj);
                    writer.print("");
                    writer.close();
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
