package com.vakedomen.events;

import com.vakedomen.core.Event;

public class LogEvent extends Event {
    String log;

    public LogEvent(String log) {
        super("log");
        this.log = log;
    }
}
