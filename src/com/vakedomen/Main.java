package com.vakedomen;
import com.google.gson.*;
import com.vakedomen.events.*;
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

    enum Algo {
        TREE,
        FLOOD
    }
    public static final Algo algo = Algo.TREE;
    public static final int ACK_WAIT_TIME = 1000;
    static final int MAX_SIM_COUNT = 50;
    static final int NETWORK_SIZE = 1300;
    static final int MINIMUM_LATENCY = 300;
    static final int MAXIMUM_LATENCY = 600;
    static final float DISCONNECT_ODDS = 0.05f;

    private static Map<WsContext, String> userUsernameMap = new ConcurrentHashMap<>();
    private static Gson gson = new Gson();
    private static ArrayList<Node> network = new ArrayList<>();
    private static boolean simEnd = false;

    public static ScheduledExecutorService executor;
    public static int informedCount;
    public static int simCount = 0;

    public static int informedChildren = 0;
    public static int brokenNodes = 0;


    public static void main(String[] args) {
	// write your code here
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
                if (ctx.message().equals("start")) run(0);
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
        //network.forEach(node -> node.deactivate());
        network = new ArrayList<>();
    }

    private static void setupGraph() {
        System.out.println("Setting up a new network...");
        generateNodes();
        syncNodes();
    }

    private static void connectNodes() {
        // pride v postev pri floodingu
    }

    private static void syncNodes() {
        for (Node n : network) {
            n.setNetwork(network);
        }
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
        if (informedCount >= NETWORK_SIZE) {
            if (!simEnd) {
                broadcastMessage(new SimSuccessfulEvent(simCount));
                simEnd = true;
            }

        }
    }

    private static void run(int delay) {
        simCount = 0;
        try {
            while (simCount <= MAX_SIM_COUNT) {
                simCount++;
                Thread.sleep(delay);
                resetGraph();
                Thread.sleep(1000);
                setupGraph();
                Thread.sleep(2000);
                for (Node n : network) {
                    if (Main.DISCONNECT_ODDS > new Random().nextFloat()) {
                        n.deactivate();
                    }
                }
                simEnd = false;
                int simEndCounter = 0;
                startSimulation();

                while (!simEnd) {
                    Thread.sleep(1000);
                    simEndCounter++;
                    if (simEndCounter > 40) {
                        simEnd = true;
                        System.out.println("simcount" + simCount);
                        broadcastMessage(new SimFialedEvent(
                                simCount,
                                network.stream().filter((Node n) -> !n.isInformed()).map((Node n) -> n.getId().substring(0,5)).collect(Collectors.toList()).size()
                            ));
                    }
                }

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
