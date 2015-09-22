package com.abborg.glom.model;

import android.graphics.Bitmap;
import android.location.Location;

import java.io.Serializable;

/**
 * Base class for all users
 *
 * Created by Boat on 8/9/58.
 */
public class User implements Serializable {

    private String name;

    private String id;

    private Location location;

    private Bitmap avatar;

    private Circle currentCircle;

    private boolean broadcastLocationEnabled;

    private boolean discoverable;

    public User(String name, String id, Location location) {
        this.name = name;
        this.id = id;
        this.location = location;
        this.currentCircle = null;
        this.broadcastLocationEnabled = false;
        this.discoverable = false;
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

    public void setAvatar(Bitmap avatar) { this.avatar = avatar; }

    public Bitmap getAvatar() { return this.avatar; }

    public void setCurrentCircle(Circle circle) { this.currentCircle = circle; }

    public Circle getCurrentCircle() { return currentCircle; }

    public void setBroadcastingLocation(boolean enabled) { broadcastLocationEnabled = enabled; }

    public boolean isBroadcastingLocation() { return broadcastLocationEnabled; }

    public void setDiscoverable(boolean enabled) { discoverable = enabled; }

    public boolean isDiscoverable() { return discoverable; }

    @Override
    public String toString() {
        return id;
    }
}
