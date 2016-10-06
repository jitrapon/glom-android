package com.abborg.glom.model;

import android.location.Location;

/**
 * Base class for all users
 * Note that Parcel does not work yet (circular reference as issued in https://github.com/johncarl81/parceler/issues/66)
 *
 * Created by Boat on 8/9/58.
 */
public class User {

    private String name;

    private String id;

    private Location location;

    private String avatar;

    private Circle currentCircle;

    private String status;

    private int type;

    private boolean dirty;

    public static final int TYPE_USER = 1;
    public static final int TYPE_ENTITY = 2;
    public static final int TYPE_BOT = 3;

    public User(String name, String id, Location location, int type) {
        this.name = name;
        this.id = id;
        this.location = location;
        this.currentCircle = null;
        this.status = null;
        this.type = type;
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

    public void setType(int type) { this.type = type; }

    public String getAvatar() { return this.avatar; }

    public void setCurrentCircle(Circle circle) { this.currentCircle = circle; }

    public Circle getCurrentCircle() { return currentCircle; }

    public void setStatus(String status) { this.status = status; }

    public String getStatus() { return status; }

    public int getType() { return type; }

    public boolean isDirty() { return dirty; }

    public void setDirty(boolean isDirty) { dirty = isDirty; }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof User)) return false;
        User otherUser = (User) other;
        if (this.id != null && otherUser.id != null) {
            return this.id.equalsIgnoreCase(otherUser.id);
        }
        else return false;
    }
}
