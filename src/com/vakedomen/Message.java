package com.vakedomen;

import com.vakedomen.helpers.Util;

public class Message {
    private String id;
    private Node generator;
    private String path;
    private Message.Type type;
    private int hop;

    public Message(Node node, String path, Type t, int hop) {
        this.id = Util.hash(System.currentTimeMillis() + "");
        this.generator = node;
        this.path = path;
        this.type = t;
        this.hop = hop;
    }

    public Message(String id, Node node, String path, Type t, int hop) {
        this.id = id;
        this.generator = node;
        this.path = path;
        this.type = t;
        this.hop = hop;
    }

    public String getId() {
        return id;
    }

    public Node getGenerator() {
        return generator;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getHop() {
        return hop;
    }

    enum Type {
        DATA,
        ACK
    }
}


