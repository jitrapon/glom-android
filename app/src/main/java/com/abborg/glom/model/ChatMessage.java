package com.abborg.glom.model;

public class ChatMessage {

    private String content;
    private boolean isMine;

    public ChatMessage(String message, boolean mine) {
        content = message;
        isMine = mine;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setIsMine(boolean isMine) {
        this.isMine = isMine;
    }
}
