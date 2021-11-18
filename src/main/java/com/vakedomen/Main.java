package com.vakedomen;
import com.google.gson.*;
import com.vakedomen.config.SimulationConfig;
import com.vakedomen.core.Event;
import com.vakedomen.core.Node;
import com.vakedomen.events.*;
import com.vakedomen.helpers.Util;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;


import static com.vakedomen.config.Flood.FLOOD_CONNECTIONS;
import static com.vakedomen.config.Flood.FLOOD_FAN_OUT;
import static com.vakedomen.config.Run.*;
import static com.vakedomen.config.Tree.ACK_WAIT_TIME;
import static com.vakedomen.core.Enums.Algo;

public class Main {

    /*
        MAP OF CLIENT SOCKETS
     */
    private static Map<WsContext, String> userUsernameMap = new ConcurrentHashMap<>();
    /*
        GSON OBJECT FOR PARSING JSON
     */
    private static Gson gson = new Gson();
    /*
        ARRAY OF NODES IN THE SIMULATED NETWORK
     */
    private static ArrayList<Node> network = new ArrayList<>();
    /*
        FLAG OF SIMULATION END - USED FOR RUNNING MULTIPLE SIMULATIONS
     */
    private static boolean simEnd = false;
    /*
        RANDOM
     */
    private static Random r = new Random();
    /*
        EXECUTOR SERVICE FOR QUEUEING MESSAGES
     */
    public static ScheduledExecutorService executor;
    /*
        COUNT OF INFORMED NODES - USED TO CHECK SIMULATION END
     */
    public static int informedCount;
    /*
        COUNTER OF SIMULATIONS ALREADY RUN
     */
    public static int simCount = 0;
    /*
        COUNT OF INACTIVE NODES IN SIMULATION
     */
    public static int brokenNodes = 0;
    /*
        COUNT OF ALL SENT MESSAGES IN SIMULATION
     */
    public static int sentMessages = 0;
    /*
        COUNT OF HOPS FOR ALL THE MESSAGES TO REACH THE NODE
     */
    public static ArrayList<Integer> hops = new ArrayList<>();
    /*
        MAXIMUM TRAVEL OF MESSAGE TO REACH A NODE
     */
    public static int maxHop = 0;
    /*
        TIMER START FOR TIMING THE SIMULATION
     */
    public static long t1;


    public static void main(String[] args) {
        /*
            INIT CSV IF SIMULATION SHOULD SAVE THE RESULTS
         */
        if (SAVE_DATA) {
            Util.createCsvFile();
            Util.log(CSV_HEADER);
        }
        resetExecutor();
        /*
            SERVE STATIC FRONTEND FILES ON PORT
         */
        Javalin app = Javalin.create(config -> {
            config.addStaticFiles(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.skipFileFunction = req -> false;
            });
        }).start(5000);

        /*
            RUN WEB SOCKET SERVER FOR FRONTEND-BACKEND COMMUNICATION
         */
        app.ws("/update", ws ->{
            /*
                CREATE CLIENT ON CONNECTION
             */
            ws.onConnect(ctx -> {
                userUsernameMap.put(ctx, "client");
                System.out.println("User connected to web socket");
            });
            /*
                REMOVE CLIENT ON DISCONNECT
             */
            ws.onClose(ctx -> {
                String username = userUsernameMap.get(ctx);
                userUsernameMap.remove(ctx);
            });
            /*
                RUN SIMULATION ON START COMMAND
             */
            ws.onMessage(ctx -> {
                if (ctx.message().equals("start")) run();
            });
        });
    }

    private static void resetGraph() {
        /*
            LOG
         */
        broadcastMessage(new ResetGraphEvent());
        System.out.println("Resetting graph...");
        /*
            VAR RESET
         */
        informedCount   = 0;
        brokenNodes     = 0;
        t1              = 0;
        sentMessages    = 0;
        maxHop          = 0;
        hops            = new ArrayList<>();
        network         = new ArrayList<>();
    }

    private static void setupGraph(SimulationConfig conf) {
        /*
            LOG
         */
        System.out.println("Setting up a new network...");
        /*
            GRAPH SETUP
         */
        resetGraph();
        generateNodes(conf);
        syncNodes(conf);
        disconnectNodes(conf);
    }

    private static int disconnectNodes(SimulationConfig conf) {
        int dcCount = 0;
        for (Node n : network) {
            /*
                DISCONNECT RANDOM NODESS
             */
            if (conf.getDisconnectOdds() > new Random().nextFloat()) {
                n.deactivate();
                dcCount++;
            }
        }
        return dcCount;
    }

    private static void syncNodes(SimulationConfig conf) {
        /*
            CHECK RUNNING ALGORITHM
         */
        switch (conf.getAlgo()) {
            /*
                SET NETWORK FOR ALL NODES (FULLY CONNECTED GRAPH)
             */
            case TREE -> {
                for (Node n : network) {
                    n.setNetwork(network);
                }
            }
            /*
                SET NEIGHBOURS FOR ALL NODES (BASED ON FLOOD CONFIG)
             */
            case FLOOD -> {
                for (Node n : network) {
                    /*
                        CONNECT NODE TO RANDOM NODES IN GRAPH
                    */
                    while (n.getNeighbours().size() < FLOOD_CONNECTIONS) {
                        Node el = Util.randomElement(network);
                        /*
                            IF CONNECTION DOES NOT ALREADY EXIST, ADD IT
                         */
                        if (n.addNeighbour(el)) {
                            el.addNeighbour(n);
                        }
                    }
                }
            }
        }
    }


    private static void generateNodes(SimulationConfig conf) {
        for (int i = 0 ; i < conf.getNetworkSize() ; i++) {
            /*
                GENERATE NODE
             */
            Node node = new Node(Util.generateNodeId(i + r.nextInt() + ""), simCount, conf);
            network.add(node);
            /*
                INFORM CLIENT
             */
            broadcastMessage(new NewNodeEvent(node));
        }
    }

    public static void broadcastMessage(Event message) {
        /*
            SEND MESSAGE TO ALL CLIENTS
         */
        userUsernameMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> {
            session.send(gson.toJson(message));
        });
    }

    private static void startSimulation() {
        if (network == null || network.isEmpty()) {
            return;
        }
        network.get(0).generateMessage();
    }

    public static void checkEndPropagation(int networkSize) {
        System.out.println("_______________________________________" +  countInformedNodes(network) + " " + networkSize + " " + !simEnd);
        if (countInformedNodes(network) == networkSize && !simEnd) {
            System.out.println("2222222222_______________________________________");
            /*
                INFORM CLIENTS OF SUCCESSFUL SIMULATION
             */
            broadcastMessage(new SimSuccessfulEvent(simCount));
            /*
                MARK END SIMULATION
             */
            t1 = System.currentTimeMillis();
            simEnd = true;
        }
    }

    private static void run() {
        /*
            BOOTSTRAP CONSTANTS
         */
        Algo[] alg              = ALGORITHMS_TO_SIMULATE;
        int[] networkSizes      = NETWORK_SIZES_TO_SIMULATE;
        float[] dcOdds          = DISCONNECT_ODDS_TO_SIMULATE;
        /*
            INIT VARS
         */
        int totalSimCount       = 0;
        int totalSims           = alg.length * networkSizes.length * dcOdds.length * MAX_SIM_COUNT;

        try {
            for (Algo algo : alg) {
                for (int size : networkSizes) {
                    for (float odds : dcOdds) {
                        /*
                            RUN SIMULATIONS
                         */
                        simCount = 0;
                        while (simCount < MAX_SIM_COUNT) {
                            /*
                                INIT SIMULATION CONFIG
                             */
                            totalSimCount++;
                            simCount++;
                            simEnd = false;
                            SimulationConfig conf = new SimulationConfig(
                                algo,
                                size,
                                odds
                            );
                            /*
                                INFORM CLIENTS
                             */
                            broadcastMessage(new LogEvent(constructNewSimulationLog(conf, totalSimCount, totalSims)));
                            /*
                                RUN SIMULATION
                             */
                            simulate(conf, simCount);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void simulate(SimulationConfig conf, int id) throws InterruptedException {
        /*
            SETUP GRAPH FOR SIMULATION
         */
        setupGraph(conf);
        Thread.sleep(2000); //timeout to allow frontend to display all nodes
        /*
            LOG START TIME OF THE SIMULATION
         */
        long t0 = System.currentTimeMillis();
        /*
            START THE SIMULATION - GENERATE THE FIRST MESSAGE
         */
        startSimulation();
        /*
            WAIT UNTIL THE SIMULATION ENDS (simEnd triggered by checkEndPropagation())
         */
        while (!simEnd) {
            /*
                END SIMULATION IF TIME RUNS OUT
             */
            if (System.currentTimeMillis() - t0 > SIM_TIME_CAP) {
                t1 = System.currentTimeMillis();
                /*
                    INFORM CLIENTS OF TIMEOUT
                 */
                broadcastMessage(new SimFialedEvent(simCount, countUninformedNodes(network)));
                break;
            }
            Thread.sleep(500);
        }
        /*
            INFORM CLIENTS OF SIMULATION STATS
         */
        broadcastMessage(new LogEvent(constructEndSimulationLog(id)));
        /*
            SAVE DATA IF NEEDED
         */
        if (SAVE_DATA) {
            Util.logArg(
                    simCount,
                    conf.getAlgo(),
                    conf.getNetworkSize(),
                    countUninformedNodes(network),
                    conf.getDisconnectOdds(),
                    countInactiveNodes(network),
                    sentMessages,
                    calcAvgHops(),
                    maxHop,
                    t1 - t0,
                    conf.getAlgo() == Algo.TREE ? 2 : FLOOD_FAN_OUT,
                    MINIMUM_LATENCY,
                    MAXIMUM_LATENCY,
                    ACK_WAIT_TIME
            );
        }
        /*
            ALLOW FRONTEND TO CLEANUP
         */
        Thread.sleep(2000);
    }

    private static void resetExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    private static String constructEndSimulationLog(int id) {
        return "-><span class='key'> Simulation  " +
                id +
                " stats: <br>    - Number of uninformed nodes: &#09;" +
                countUninformedNodes(network) +
                "<br>    - Average hops: &#09;&#09;&#09;" +
                calcAvgHops() +
                "<br>    - Max hops: &#09;&#09;&#09;" +
                maxHop +
                " <br>    - Sent messages: &#09;&#09;&#09;" +
                sentMessages +
                " </span>";
    }

    private static String constructNewSimulationLog(SimulationConfig conf, int totalSimCount, int totalSims) {
        return "-><span class='key'> Simulation  " +
                totalSimCount +
                "/" +
                totalSims +
                " started with settings:<br>     - ALGORITHM: &#09;&#09;" +
                conf.getAlgo() +
                "<BR>     - NETWORK SIZE: &#09;&#09;" +
                conf.getNetworkSize() +
                "<br>     - DISCONNECT ODDS: &#09;" +
                conf.getDisconnectOdds() +
                "<br>     - REPETITION: &#09;&#09;" +
                simCount +
                "/" +
                MAX_SIM_COUNT;
    }

    private static int countUninformedNodes(ArrayList<Node> network) {
        return network.stream().filter((Node n) -> !n.isInformed()).map((Node n) -> n.getId().substring(0,5)).collect(Collectors.toList()).size();
    }

    private static int countInformedNodes(ArrayList<Node> network) {
        return network.stream().filter((Node n) -> n.isInformed() || !n.isActive() ).map((Node n) -> n.getId().substring(0,5)).collect(Collectors.toList()).size();
    }

    private static int countInactiveNodes(ArrayList<Node> network) {
        return network.stream().filter((Node n) -> !n.isActive()).map((Node n) -> n.getId().substring(0,5)).collect(Collectors.toList()).size();
    }

    private static float calcAvgHops() {
        return (hops.stream().reduce(0, (a, b) -> a + b) * 1.f) / (hops.size() * 1.f);
    }
}
