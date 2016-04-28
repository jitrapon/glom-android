package com.abborg.glom.model;

/**
 * Base class for all messages
 */
public abstract class BaseChatMessage {

    private String id;
    private String content;
    private boolean isMine;
    private User sender;
    private OutgoingStatus outgoingStatus;
    private ContentStatus contentStatus;

    /**
     * Status of the outgoing message
     */
    public enum OutgoingStatus {
        SENDING,
        SERVER_RECEIVED,
        DEVICE_RECEIVED,
        READ,
        FAILED,
        UNKNOWN
    }

    /**
     * Status of the content of the message
     */
    public enum ContentStatus {
        UNMODIFIED,
        EDITED,
        DELETED
    }

    public BaseChatMessage(String messageId, String message, User user, boolean mine) {
        id = messageId;
        content = message;
        isMine = mine;
        sender = user;
        if (isMine) outgoingStatus = OutgoingStatus.SENDING;
        else outgoingStatus = OutgoingStatus.UNKNOWN;
        contentStatus = ContentStatus.UNMODIFIED;
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

    public void setOutgoingStatus(OutgoingStatus status) { outgoingStatus = status; }

    public OutgoingStatus getOutgoingStatus() { return outgoingStatus; }

    public void setContentStatus(ContentStatus status) { contentStatus = status; }

    public ContentStatus getContentStatus() { return contentStatus; }
}
