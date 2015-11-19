package com.abborg.glom.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all users
 * Note that Parcel does not work yet (circular reference as issued in https://github.com/johncarl81/parceler/issues/66)
 *
 * Created by Boat on 8/9/58.
 */
public class User implements Parcelable {

    private String name;

    private String id;

    private Location location;

    private String avatar;

    private Circle currentCircle;

    private List<Integer> userPerm;

    private String status;

    public static final int MEDIA_IMAGE_RECEIVE = 1;

    public static final int MEDIA_AUDIO_RECEIVE = 2;

    public static final int MEDIA_VIDEO_RECEIVE = 3;

    public static final int ALARM_RECEIVE = 4;

    public static final int NOTE_RECEIVE = 5;

    public static final int LOCATION_REQUEST_RECEIVE = 6;

    public static final int CREATE_EVENT = 7;

    public User(String name, String id, Location location) {
        this.name = name;
        this.id = id;
        this.location = location;
        this.currentCircle = null;
        userPerm = new ArrayList<>();
        this.status = null;
    }

    public User(Parcel parcel) {
        readFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(id);
        location.writeToParcel(dest, flags);
        dest.writeString(avatar);
        dest.writeParcelable(currentCircle, flags);
        dest.writeList(userPerm);
        dest.writeString(status);
    }

    private void readFromParcel(Parcel in) {
        // needs to be in the same order as when they are written
        name = in.readString();
        id = in.readString();
        location = Location.CREATOR.createFromParcel(in);
        avatar = in.readString();
        currentCircle = in.readParcelable(Circle.class.getClassLoader());
        userPerm = new ArrayList<>();
        in.readList(userPerm, Integer.class.getClassLoader());
        status = in.readString();
    }

    public static final Parcelable.Creator CREATOR =
        new Parcelable.Creator() {
            public User createFromParcel(Parcel in) {
                return new User(in);
            }

            public User[] newArray(int size) {
                return new User[size];
            }
        };

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

    public void setUserPermission(List<Integer> userPerm) {
        this.userPerm = userPerm;
    }

    public List<Integer> getUserPermission() {
        return userPerm;
    }

    public void setStatus(String status) { this.status = status; }

    public String getStatus() { return status; }

    @Override
    public String toString() {
        return id;
    }
}
