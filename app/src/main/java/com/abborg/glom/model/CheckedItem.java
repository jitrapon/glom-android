package com.abborg.glom.model;

/**
 * Created by jitrapon
 */
public class CheckedItem {

    private String text;
    private int state;

    public CheckedItem(int state, String text) {
        this.text = text;
        this.state = state;
    }

    public String getText() { return text; }

    public void setText(String text) { this.text = text; }

    public int getState() { return state; }

    public void setState(int state) {
        this.state = state;
    }
}
