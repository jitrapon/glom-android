package com.abborg.glom.model;

public class CircleInfo implements NavMenuItem {

    public String id;
    public String name;
    public String avatar;
    public String info;
    public int numUsers;

    @Override
    public int getType() {
        return TYPE_ITEM;
    }
}
