package com.abborg.glom.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Boat on 9/9/58.
 *
 * A circle is a unit of room of users.
 * This class is also responsible for each row in navigation drawer menu of circles
 *
 * Note that Parcel does not work yet (circular reference as issued in https://github.com/johncarl81/parceler/issues/66)
 */
public class Circle implements Parcelable {

    /**
     * List of user in this circle
     */
    private List<User> users;

    /**
     * User-defined title
     */
    private String title;

    /**
     * Unique ID generated
     */
    private String id;

    /**
     * Nav-drawer notify
     */
    private boolean showNotify;

    /**
     * Whether or not the current user is broadcasting location in this circle
     */
    private boolean userIsBroadcastingLocation;

    /**
     * Whether or not the current user is discoverable in this circle
     */
    private boolean userIsDiscoverable;

    /**
     * List of saved events in this circle
     */
    private List<Event> events;


    /**
     * Create a new circle with this user in it
     * TODO throw exception by checking SQLite and server if the circle's title already exists
     *
     * @param title The user-defined title of the new circle to be created
     * @return The created circle instance if the title is not created before
     */
    public static Circle createCircle(String title, User user) {
        String id = generateCircleId();

        //TODO add to SQLITE and update server
        ArrayList<User> users = new ArrayList<>(Arrays.asList(user));
        return new Circle(id, title, users);
    }

    public Circle(Parcel parcel) {
        readFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(users);
        dest.writeString(title);
        dest.writeString(id);
        dest.writeByte((byte) (showNotify ? 1 : 0));
        dest.writeByte((byte) (userIsBroadcastingLocation ? 1 : 0));
        dest.writeByte((byte) (userIsDiscoverable ? 1 : 0));
    }

    private void readFromParcel(Parcel in) {
        // needs to be in the same order as when they are written
        users = new ArrayList<>();
        in.readTypedList(users, User.CREATOR);
        title = in.readString();
        id = in.readString();
        showNotify = in.readByte() != 0;
        userIsBroadcastingLocation = in.readByte() != 0;
        userIsDiscoverable = in.readByte() != 0;
    }

    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {
                public Circle createFromParcel(Parcel in) {
                    return new Circle(in);
                }

                public Circle[] newArray(int size) {
                    return new Circle[size];
                }
            };

    /**
     * Call this in DEBUG only to override the generated ID
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    private Circle(String id, String title, List<User> users) {
        this.id = id;
        this.title = title;
        this.users = users;
        this.showNotify = false;
        this.userIsBroadcastingLocation = false;
        this.userIsDiscoverable = false;
        this.events = new ArrayList<>();
    }

    public boolean isShowNotify() {
        return showNotify;
    }

    public void setShowNotify(boolean showNotify) {
        this.showNotify = showNotify;
    }

    public List<User> getUsers() { return users; }

    public String getUserListString() {
        StringBuilder userList = new StringBuilder();
        for (User user : users) {
            userList.append(user.getId());
            userList.append(",");
        }
        return userList.length() > 0 ? userList.substring(0, userList.length() - 1) : "";
    }

    public String getTitle() { return title; }

    public String getId() { return id; }

    public void setTitle(String newTitle) { title = newTitle; }

    public void addUser(User user) { users.add(user); }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public void addUsers(List<User> users) {
        this.users.addAll(users);
    }

    private static String generateCircleId() {
        return String.valueOf(UUID.randomUUID());
    }

    public void setBroadcastingLocation(boolean enabled) {
        userIsBroadcastingLocation = enabled;
    }

    public boolean isUserBroadcastingLocation() {
        return userIsBroadcastingLocation;
    }

    public List<Event> getEvents() { return events; }

    public void addEvent(Event event) {
        events.add(event);
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return title;
    }
}
