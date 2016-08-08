package com.abborg.glom.model;

import org.joda.time.DateTime;

/**
 * Created by Jitrapon
 */
public class FeedAction {

    public static final int CREATE = 0;
    public static final int CANCELED = 1;
    public static final int EDITED = 2;

    public int type;

    public User user;

    public DateTime dateTime;

    public FeedAction(int type, User user, DateTime dateTime) {
        this.type = type;
        this.user = user;
        this.dateTime = dateTime;
    }
}
