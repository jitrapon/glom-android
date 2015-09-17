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
        if (title == null) return null;

        String id = generateCircleId();

        if (!titleAlreadyExists(title)) {
            //TODO add to SQLITE and update server
            ArrayList<User> users = new ArrayList<User>(Arrays.asList(user));
            return new Circle(id, title, users);
        }
        else {
            //TODO throws DuplicateCircleTitleException
        }
        return null;
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

    public String getTitle() { return title; }

    public String getId() { return id; }

    public void setTitle(String newTitle) { title = newTitle; }

    public void addUser(User user) { users.add(user); }

    public void addUsers(List<User> users) {
        ArrayList<User> combine = new ArrayList<User>();
        combine.addAll(this.users);
        combine.addAll(users);
    }

    private static String generateCircleId() {
        return String.valueOf(UUID.randomUUID());
    }

    //TODO
    public static boolean titleAlreadyExists(String title) {
        return false;
    }
}
