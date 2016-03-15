package com.abborg.glom.model;

import org.joda.time.DateTime;

import java.util.UUID;

public class BoardItem {

    protected boolean dirty;

    protected String id;

    protected int type;

    protected DateTime createdTime;

    protected DateTime updatedTime;

    protected Circle circle;

    protected FeedAction info;

    public static final int TYPE_EVENT = 1;

    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public boolean isDirty() { return dirty; }

    public void setLastAction(FeedAction info) {
        this.info = info;
    }

    public FeedAction getLastAction() { return info; }

    protected static String generateId() {
        return String.valueOf(UUID.randomUUID());
    }

    public String getId() { return id; }

    public Circle getCircle() { return circle; }

    public int getType() { return type; }

    public DateTime getCreatedTime() { return createdTime; }

    public void setCreatedTime(DateTime date) { createdTime = date; }

    public DateTime getUpdatedTime() { return updatedTime; }

    public void setUpdatedTime(DateTime date) { updatedTime = date; }
}
