package com.vakedomen;
import com.google.gson.*;
import com.vakedomen.events.*;
import com.vakedomen.helpers.Util;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class Main {

    public static final String FILE_NAME = "data.csv";

    public enum Algo {
        TREE,
        FLOOD
    }

    public static final boolean CLEAR_DATA = false;
    public static final boolean SAVE_DATA = false;

    public static Algo ALGO = Algo.TREE;
    public static int ACK_WAIT_TIME = 1100;
    public static int MAX_SIM_COUNT = 10;
    public static int NETWORK_SIZE = 1500;
    public static int MINIMUM_LATENCY = 300;
    public static int MAXIMUM_LATENCY = 500;
    public static float DISCONNECT_ODDS = 0.15f;
    public static int FLOOD_CONNECTIONS = 7;
    public static int FLOOD_FAN_OUT = 5;

    private static Map<WsContext, String> userUsernameMap = new ConcurrentHashMap<>();
    private static Gson gson = new Gson();
    private static ArrayList<Node> network = new ArrayList<>();
    private static boolean simEnd = false;
    private static Random r = new Random();

    public static ScheduledExecutorService executor;
    public static int informedCount;
    public static int simCount = 0;

    public static int informedChildren = 0;
    public static int brokenNodes = 0;
    public static int sentMessages = 0;
    public static ArrayList<Integer> hops = new ArrayList<Integer>();
    public static int maxHop = 0;
    public static long t1;


    public static void main(String[] args) {
        if (SAVE_DATA) {
            Util.createCsvFile();
            Util.log("SIM_ID;ALG;NODE_COUNT;UNINFORMED_COUNT;DISCONNECT_ODDS;DISCONNECT_COUNT;MSG_SENT;AVG_HOP_COUNT;MAX_HOP_COUNT;TOTAL_TIME_MILLIS;FAN_OUT;MIN_LATENCY;MAX_LATENCY;ACK_WAIT_TIME");

        }
	    executor = Executors.newSingleThreadScheduledExecutor();
        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("resources/public");
        }).start(5000);
        app.ws("/update", ws ->{

            ws.onConnect(ctx -> {
                userUsernameMap.put(ctx, "client");
                System.out.println("User connected to web socket");
            });
            ws.onClose(ctx -> {
                String username = userUsernameMap.get(ctx);
                userUsernameMap.remove(ctx);
            });

            ws.onMessage(ctx -> {
                if (ctx.message().equals("start")) run();
            });
        });
    }

    private static void resetGraph() {
        System.out.println("Resetting graph...");
        executor.shutdownNow();
        executor = Executors.newSingleThreadScheduledExecutor();
        broadcastMessage(new ResetGraphEvent());
        informedCount = 0;
        informedChildren = 0;
        brokenNodes = 0;
        t1 = 0;
        sentMessages = 0;
        hops = new ArrayList<>();
        maxHop = 0;
        network = new ArrayList<>();
    }

    private static void setupGraph() {
        System.out.println("Setting up a new network...");
        generateNodes();
        syncNodes();
    }

    private static void syncNodes() {
        switch (ALGO) {
            case TREE -> {
                for (Node n : network) {
                    n.setNetwork(network);
                }
            }
            case FLOOD -> {
                for (Node n : network) {

                    while (n.getNeighbours().size() < FLOOD_CONNECTIONS) {
                        Node el = randomElement(network);
                        if (n.addNeighbour(el)) {
                            el.addNeighbour(n);
                        }
                    }
                }
            }
        }
    }

    private static Node randomElement(ArrayList<Node> arrayList) {
        int index = r.nextInt(arrayList.size());
        return arrayList.get(index);
    }

    private static void generateNodes() {
        for (int i = 0 ; i < NETWORK_SIZE ; i++) {
            long t = System.currentTimeMillis();
            Node node = new Node(Util.generateNodeId(i + t + ""), simCount);
            network.add(node);
            Event newNodeEvent = new NewNodeEvent(node);
            broadcastMessage(newNodeEvent);
        }
    }

    public static void broadcastMessage(Event message) {
        userUsernameMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> {
            session.send(gson.toJson(message));
        });
    }

    private static void startSimulation() {
        network.get(0).generateMessage();
    }

    public static void checkEndPropagation() {
        if (informedCount > Math.floor(NETWORK_SIZE * 0.99)) {
            List<String> f = network.stream().filter((Node n) -> !n.isInformed()).map((Node n) -> n.getId().substring(0,5)).collect(Collectors.toList());
            System.out.println("[MAIN] still to inform: " + f.stream().map((String s) -> s.substring(0, 5)).collect(Collectors.toList()));
        }
        t1 = System.currentTimeMillis();
        if (informedCount >= NETWORK_SIZE) {
            if (!simEnd) {
                broadcastMessage(new SimSuccessfulEvent(simCount));
                simEnd = true;
            }

        }
    }

    private static void run() {
        int totalSimCount = 0;
        Algo[] alg = {Algo.TREE, Algo.FLOOD};
        int[] networkSizes = {100, 500, 1000, 2000};
        float[] dcOdds = {0f, 0.05f, 0.1f, 0.25f, 0.5f, 0.75f };
        int totalSims = alg.length * networkSizes.length * dcOdds.length * MAX_SIM_COUNT;
        try {
        /*    for (Algo algo : alg) {
                for (int size : networkSizes) {
                    for (float odds : dcOdds) {
                        ALGO = algo;
                        NETWORK_SIZE = size;
                        DISCONNECT_ODDS = odds;*/
                        simCount = 0;
                        while (simCount < MAX_SIM_COUNT) {
                            totalSimCount++;
                            simCount++;
                            broadcastMessage(new LogEvent("-><span class='key'> Simulation  " + totalSimCount + "/" + totalSims + " started with settings:<br>     - ALGORITHM: &#09;&#09;" + ALGO + "<BR>     - NETWORK SIZE: &#09;&#09;" + NETWORK_SIZE + "<br>     - DISCONNECT ODDS: &#09;" + DISCONNECT_ODDS + "<br>     - REPETITION: &#09;&#09;" + simCount + "/" + MAX_SIM_COUNT));
                            sim(totalSimCount);
                        }
                   /* }
                }
            }*/
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void sim(int id) throws InterruptedException {
        resetGraph();
        setupGraph();
        Thread.sleep(2000);
        int dcCount = 0;
        for (Node n : network) {
            if (Main.DISCONNECT_ODDS > new Random().nextFloat()) {
                n.deactivate();
                dcCount++;
            }
        }
        simEnd = false;
        int simEndCounter = 0;
        long t0 = System.currentTimeMillis();
        startSimulation();
        int uninformed = 0;
        while (!simEnd) {
            Thread.sleep(1000);
            simEndCounter++;
            if (simEndCounter > 60) {
                uninformed = network.stream().filter((Node n) -> !n.isInformed()).map((Node n) -> n.getId().substring(0,5)).collect(Collectors.toList()).size();
                simEnd = true;
                broadcastMessage(new SimFialedEvent(simCount, 0));
            }
        }

        float avgHops = (hops.stream().reduce(0, (a, b) -> a + b) * 1.f) / (hops.size() * 1.f);
        broadcastMessage(new LogEvent("-><span class='key'> Simulation  " + id + " stats: <br>    - Number of uninformed nodes: &#09;" + uninformed +"<br>    - Average hops: &#09;&#09;&#09;" + avgHops + "<br>    - Max hops: &#09;&#09;&#09;" + maxHop + " <br>    - Sent messages: &#09;&#09;&#09;" + sentMessages + " </span>"));
        Util.logArg(
                simCount,
                ALGO,
                NETWORK_SIZE,
                uninformed,
                DISCONNECT_ODDS,
                dcCount,
                sentMessages,
                avgHops,
                maxHop,
                t1 - t0,
                ALGO == Algo.TREE ? 2 : FLOOD_FAN_OUT,
                MINIMUM_LATENCY,
                MAXIMUM_LATENCY,
                ACK_WAIT_TIME
        );
        Thread.sleep(1000);
    }
}
