package com.abborg.glom.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Boat on 9/9/58.
 *
 * A circle is a unit of room of users.
 * This class is also responsible for each row in navigation drawer menu of circles
 */
public class Circle {

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
     * Create a new circle with this user in it
     * TODO throw exception by checking SQLite and server if the circle's title already exists
     *
     * @param title The user-defined title of the new circle to be created
     * @return The created circle instance if the title is not created before
     */
    public static Circle createCircle(String title, User user) {
        String id = generateCircleId();

        //TODO add to SQLITE and update server
        ArrayList<User> users = new ArrayList<User>(Arrays.asList(user));
        return new Circle(id, title, users);
    }

    /**
     * Check if the specified user ID is in the specified circle ID
     *
     * @param id
     * @param circle
     * @return
     */
    public static boolean circleContainsUserId(String id, Circle circle) {
        if (circle == null || id == null) return false;

        for (User user : circle.getUsers()) {
            if (user.getId().equals(id)) return true;
        }
        return false;
    }

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

    @Override
    public String toString() {
        return title;
    }
}
