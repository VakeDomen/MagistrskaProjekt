package com.vakedomen;

import java.util.Random;

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
}
