package com.vakedomen;

public class Message {
    private String id;
    private Node generator;
    private String path;
    private Message.Type type;

    public Message(Node node, String path, Type t) {
        this.id = Util.hash(System.currentTimeMillis() + "");
        this.generator = node;
        this.path = path;
        this.type = t;
    }

    public Message(String id, Node node, String path, Type t) {
        this.id = id;
        this.generator = node;
        this.path = path;
        this.type = t;
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

    enum Type {
        DATA,
        ACK
    }
}


