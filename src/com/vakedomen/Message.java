package com.vakedomen;

public class Message {
    private String id;
    private Node generator;
    private String path;

    public Message(String id, Node nodeId, String path) {
        this.id = id;
        this.generator = nodeId;
        this.path = path;
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
}
