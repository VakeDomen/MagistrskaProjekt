package com.vakedomen.events;

public class SimSuccessfulEvent extends Event {
    int simNumber;

    public SimSuccessfulEvent(int simNumber) {
        super("sim_successful");
        this.simNumber = simNumber;
    }
}
