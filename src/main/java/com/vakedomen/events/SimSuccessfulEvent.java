package com.vakedomen.events;

import com.vakedomen.core.Event;

public class SimSuccessfulEvent extends Event {
    int simNumber;

    public SimSuccessfulEvent(int simNumber) {
        super("sim_successful");
        this.simNumber = simNumber;
    }
}
