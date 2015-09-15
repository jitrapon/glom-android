package com.abborg.glom.model;

import java.util.List;

/**
 * Created by Boat on 9/9/58.
 *
 * A circle is a unit of room of users. It must contain at least one user in it.
 */
public class Circle {

    private List<User> users;

    private String name;

    private String id;

    public Circle createCircle(String id, String name, List<User> users) {
        return new Circle(id, name, users);
    }

    private Circle(String id, String name, List<User> users) {
        this.id = id;
        this.name = name;
        this.users = users;
    }


}
