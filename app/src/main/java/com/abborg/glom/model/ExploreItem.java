package com.abborg.glom.model;

import org.joda.time.DateTime;

public abstract class ExploreItem {

    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_MOVIE = 1;
    public static final int TYPE_EVENT = 2;
    public static final int TYPE_FOOD = 3;

    protected String id;
    protected DateTime createdTime;
    protected DateTime updatedTime;

    public String getId() { return id; }

    public DateTime getCreatedTime() { return createdTime; }

    public DateTime getUpdatedTime() { return updatedTime; }

    public abstract int getType();

    public ExploreItem() {}

    public ExploreItem(String itemId, DateTime created, DateTime updated) {
        id = itemId;
        createdTime = created;
        updatedTime = updated;
    }
}
