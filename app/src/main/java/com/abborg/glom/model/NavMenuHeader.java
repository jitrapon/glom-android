package com.abborg.glom.model;

public class NavMenuHeader implements NavMenuItem {

    String title;

    @Override
    public int getType() {
        return TYPE_HEADER;
    }

    public String getTitle() { return title; }

    public NavMenuHeader(String title) {
        this.title = title;
    }
}
