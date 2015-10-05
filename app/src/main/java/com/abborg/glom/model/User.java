package com.abborg.glom.model;

import android.location.Location;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all users
 *
 * Created by Boat on 8/9/58.
 */
public class User implements Serializable {

    private String name;

    private String id;

    private Location location;

    private String avatar;

    private Circle currentCircle;

    private boolean broadcastLocationEnabled;

    private boolean discoverable;

    private List<Integer> userPerm;

    public static final int MEDIA_IMAGE_RECEIVE = 1;

    public static final int MEDIA_AUDIO_RECEIVE = 2;

    public static final int MEDIA_VIDEO_RECEIVE = 3;

    public static final int ALARM_RECEIVE = 4;

    public static final int NOTE_RECEIVE = 5;

    public static final int LOCATION_REQUEST_RECEIVE = 6;

    public static final int SHOUT_RECEIVE = 7;

    public static final int SECRET_MESSAGE = 8;

    public static final int SONG_SNIPPET_RECEIVE = 9;

    public static final int POLL_RECEIVE = 10;

    public User(String name, String id, Location location) {
        this.name = name;
        this.id = id;
        this.location = location;
        this.currentCircle = null;
        this.broadcastLocationEnabled = false;
        this.discoverable = false;
        userPerm = new ArrayList<Integer>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getAvatar() { return this.avatar; }

    public void setCurrentCircle(Circle circle) { this.currentCircle = circle; }

    public Circle getCurrentCircle() { return currentCircle; }

    public void setBroadcastingLocation(boolean enabled) { broadcastLocationEnabled = enabled; }

    public boolean isBroadcastingLocation() { return broadcastLocationEnabled; }

    public void setDiscoverable(boolean enabled) { discoverable = enabled; }

    public boolean isDiscoverable() { return discoverable; }

    public void setUserPermission(List<Integer> userPerm) {
        this.userPerm = userPerm;
    }

    public List<Integer> getUserPermission() {
        return userPerm;
    }

    @Override
    public String toString() {
        return id;
    }
}
