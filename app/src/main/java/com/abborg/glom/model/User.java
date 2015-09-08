package com.abborg.glom.model;

import android.location.Location;

import java.io.Serializable;

/**
 * Base class for all users
 *
 * Created by Boat on 8/9/58.
 */
public class User implements Serializable {

    private String fullName;

    private String funName;

    private Location location;

    public User(String fullName, String name, Location location) {
        this.fullName = fullName;
        this.funName = name;
        this.location = location;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFunName(String name) {
        this.funName = name;
    }

    public String getFunName() {
        return funName;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
