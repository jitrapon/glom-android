package com.abborg.glom.model;

public abstract class BaseChatMessage {

    private String id;
    private String content;
    private boolean isMine;
    private User sender;

    public BaseChatMessage(String messageId, String message, User user, boolean mine) {
        id = messageId;
        content = message;
        isMine = mine;
        sender = user;
    }

    public abstract String getType();

    public String getContent() {
        return content;
    }

    public String getId() { return id; }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isMine() {
        return isMine;
    }

    public User getSender() { return sender; }
}
