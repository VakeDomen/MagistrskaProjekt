package com.vakedomen.events;

public class SimFialedEvent extends Event {
    int simNumber;
    int uninformed;

    public SimFialedEvent(int simNumber, int uninformed) {
        super("sim_failed");
        this.simNumber = simNumber;
        this.uninformed = uninformed;
    }
}
