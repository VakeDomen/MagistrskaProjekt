package com.vakedomen.config;

import com.vakedomen.core.Enums;

public class SimulationConfig {
    private Enums.Algo algo;
    private int networkSize;

    public SimulationConfig(Enums.Algo algo, int networkSize, float disconnectOdds) {
        this.algo = algo;
        this.networkSize = networkSize;
        this.disconnectOdds = disconnectOdds;
    }

    private float disconnectOdds;


    public Enums.Algo getAlgo() {
        return algo;
    }

    public void setAlgo(Enums.Algo algo) {
        this.algo = algo;
    }

    public int getNetworkSize() {
        return networkSize;
    }

    public void setNetworkSize(int networkSize) {
        this.networkSize = networkSize;
    }

    public float getDisconnectOdds() {
        return disconnectOdds;
    }

    public void setDisconnectOdds(float disconnectOdds) {
        this.disconnectOdds = disconnectOdds;
    }
}
