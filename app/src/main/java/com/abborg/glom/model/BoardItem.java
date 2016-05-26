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

    protected int sync;

    /* if this item is an attachment, it will not show up in the list of board items */
    /* Attachments are items that are linked (attached) to a board item */
    protected boolean isAttachment;

    /** Board item types - this must match with what is stored server-side **/
    public static final int TYPE_EVENT = 1;
    public static final int TYPE_FILE = 2;
    public static final int TYPE_NOTE = 3;

    /** Board item sync status **/
    public static final int NO_SYNC = 0;            // indicates that this item is marked for no syncing with the server
    public static final int SYNC_COMPLETE = 1;      // indicates that this item has been synced with the server
    public static final int SYNC_ERROR = 2;         // indicates that this item is not synced, and may not be up-to-date
    public static final int SYNC_IN_PROGRESS = 3;   // indicates that this item is in the process of syncing

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

    public int getSyncStatus() { return sync; }

    public void setSyncStatus(int status) { sync = status; }

    public boolean isAttachment() { return isAttachment; }

    public void setIsAttachment(boolean enable) { isAttachment = enable; }
}
