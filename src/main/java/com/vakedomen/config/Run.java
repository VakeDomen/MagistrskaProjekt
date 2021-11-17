package com.vakedomen.config;

public class Run {

    /*
        SIZE OF THE NETWORK (NUM OF NODES)
     */
    public static int NETWORK_SIZE = 1500;
    /*
        COUNT OF SIMULATIONS TO RUN (FOR EACH CONFIGURATION)
     */
    public static int MAX_SIM_COUNT = 10;
    /*
        MINIMUM LIMIT OF LATENCY FOR ALIVE NODES
     */
    public static int MINIMUM_LATENCY = 300;
    /*
        MAXIMUM LIMIT OF LATENCY FOR ALIVE NODES
     */
    public static int MAXIMUM_LATENCY = 500;
    /*
        PROBABILITY OF A RANDOM NODE TO BE OFFLINE
     */
    public static float DISCONNECT_ODDS = 0.15f;
    /*
        COLLECT AND SAVE THE SIMULATION DATA
     */
    public static final boolean SAVE_DATA = false;
    /*
        OVERRIDE POTENTIAL ALREADY EXISTING FILES
     */
    public static final boolean CLEAR_DATA = false;
    /*
        NAME OF THE FILE TO WRITE DATA TO
     */
    public static final String FILE_NAME = "data.csv";
    /*
        SAVE DATA CSV HEADER
     */
    public static final String CSV_HEADER = "SIM_ID;ALG;NODE_COUNT;UNINFORMED_COUNT;DISCONNECT_ODDS;DISCONNECT_COUNT;MSG_SENT;AVG_HOP_COUNT;MAX_HOP_COUNT;TOTAL_TIME_MILLIS;FAN_OUT;MIN_LATENCY;MAX_LATENCY;ACK_WAIT_TIME";

}
