package com.abborg.glom.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Boat on 9/9/58.
 *
 * A circle is a unit of room of users.
 *
 * (circular reference as issued in https://github.com/johncarl81/parceler/issues/66)
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
     * Whether or not the current user is broadcasting location in this circle
     */
    private AtomicBoolean userIsBroadcastingLocation;

    /**
     * List of saved items in this circle
     */
    private List<BoardItem> items;

    /***********************************************************************************
     * INITIALIZATIONS
     ***********************************************************************************/

    private Circle(String id, String title, List<User> users) {
        this.id = id;
        this.title = title;
        this.users = Collections.synchronizedList(users);
        this.items =  Collections.synchronizedList(new ArrayList<BoardItem>());
        this.userIsBroadcastingLocation = new AtomicBoolean(false);
    }

    public static Circle createCircle(String title, User user) {
        String id = generateCircleId();
        return new Circle(id, title, Arrays.asList(user));
    }

    /***********************************************************************************
     * CIRCLE INFO
     ***********************************************************************************/

    public String getTitle() { return title; }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public void setTitle(String newTitle) { title = newTitle; }

    private static String generateCircleId() {
        return String.valueOf(UUID.randomUUID());
    }

    public void setBroadcastingLocation(boolean enabled) {
        userIsBroadcastingLocation.set(enabled);
    }

    public boolean isUserBroadcastingLocation() {
        return userIsBroadcastingLocation.get();
    }

    @Override
    public String toString() {
        return title;
    }

    /***********************************************************************************
     * USERS OPERATIONS
     ***********************************************************************************/

    public User getUser(String id) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equalsIgnoreCase(id)) return users.get(i);
        }

        return null;
    }

    public List<User> getUsers() { return users; }

    public String getUserListString() {
        StringBuilder userList = new StringBuilder();
        for (int i = 0; i < users.size(); i++) {
            userList.append(users.get(i).getId());
            userList.append(",");
        }
        return userList.length() > 0 ? userList.substring(0, userList.length() - 1) : "";
    }

    public void addUser(User toAdd) { users.add(toAdd); }

    public void addUsers(List<User> toAdd) {
        users.addAll(toAdd);
    }

    public void setUsers(List<User> newUsers) {
        users = Collections.synchronizedList(newUsers);
    }

    public void removeDirtyUsers() {
        ListIterator<User> iterator = users.listIterator();
        while (iterator.hasNext()) {
            User user = iterator.next();
            if (user.isDirty()) {
                iterator.remove();
            }
        }
    }

    /***********************************************************************************
     * ITEMS OPERATIONS
     ***********************************************************************************/

    public List<BoardItem> getItems() { return items; }

    public void addItem(BoardItem item) {
        items.add(0, item);
    }

    public void removeItem(BoardItem item) {
        items.remove(item);
    }

    public void setItems(List<BoardItem> items) {
        this.items = Collections.synchronizedList(items);
    }

    public void removeDirtyItems() {
        ListIterator<BoardItem> iterator = items.listIterator();
        while (iterator.hasNext()) {
            BoardItem item = iterator.next();
            if (item.isDirty()) {
                iterator.remove();
            }
        }
    }
}
