package com.vakedomen;
import com.google.gson.*;
import com.vakedomen.events.*;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class Main {

    static final int MAX_SIM_COUNT = 5;
    static final int NETWORK_SIZE = 16;
    static final int MINIMUM_LATENCY = 300;
    static final int MAXIMUM_LATENCY = 600;
    static final float DISCONNECT_ODDS = 0.00f;

    private static Map<WsContext, String> userUsernameMap = new ConcurrentHashMap<>();
    private static Gson gson = new Gson();
    private static ArrayList<Node> network = new ArrayList<>();

    public static ScheduledExecutorService executor;
    public static int informedCount;
    public static int simCount = 0;



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
                if (ctx.message().equals("start")) run();
            });
        });
    }

    private static void resetGraph() {
        System.out.println("Resetting graph...");
        broadcastMessage(new ResetGraphEvent());
        informedCount = 0;
        simCount++;
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
            Node node = new Node(Util.generateNodeId(i), simCount);
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
        //List<String> f = network.stream().filter((Node n) -> !n.isInformed()).map((Node n) -> n.getId().substring(0,5)).collect(Collectors.toList());
        if (informedCount >= NETWORK_SIZE) {
            if (simCount < MAX_SIM_COUNT) run();
        }
    }

    private static void run() {
        try {
            resetGraph();
            Thread.sleep(1000);
            setupGraph();
            Thread.sleep(2000);
            startSimulation();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
