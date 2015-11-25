package com.abborg.glom.model;

import org.joda.time.DateTime;

/**
 * Created by Boat on 14/10/58.
 */
public class FeedAction {

    public static final int CREATE_EVENT = 0;

    public static final int CANCEL_EVENT = 1;

    public static final int UPDATE_EVENT = 2;

    public int type;

    public User user;

    public DateTime dateTime;

    public FeedAction(int type, User user, DateTime dateTime) {
        this.type = type;
        this.user = user;
        this.dateTime = dateTime;
    }
}
