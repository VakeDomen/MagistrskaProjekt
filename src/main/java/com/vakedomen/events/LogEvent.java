package com.vakedomen.events;

import java.util.List;

public class LogEvent extends Event {
    String log;

    public LogEvent(String log) {
        super("log");
        this.log = log;
    }
}
