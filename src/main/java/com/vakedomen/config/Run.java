package com.vakedomen.config;

import com.vakedomen.core.Enums;

public class Run {

    /*
        COUNT OF SIMULATIONS TO RUN (FOR EACH CONFIGURATION)
     */
    public static int MAX_SIM_COUNT = 1;
    /*
        ALGORITHMS TO SIMULATE
     */
    public static Enums.Algo[] ALGORITHMS_TO_SIMULATE = {Enums.Algo.TREE, Enums.Algo.FLOOD};
    /*
        SIZE OF THE NETWORKS (NUM OF NODES)
     */
    public static int[] NETWORK_SIZES_TO_SIMULATE = { 100, 500, 1000, 2000 };
    /*
        DISCONNECT ODDS FOR EACH NODE
     */
    public static float[] DISCONNECT_ODDS_TO_SIMULATE = { 0f, 0.05f, 0.1f, 0.25f, 0.5f, 0.75f };
    /*
        MAXIMUM TIME[ms] CAP FOR A SIMULATION TO COMPLETE
     */
    public static final int SIM_TIME_CAP = 60000;
    /*
        MINIMUM LIMIT OF LATENCY FOR ALIVE NODES
     */
    public static int MINIMUM_LATENCY = 300;
    /*
        MAXIMUM LIMIT OF LATENCY FOR ALIVE NODES
     */
    public static int MAXIMUM_LATENCY = 500;
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
