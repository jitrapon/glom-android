package com.abborg.glom.model;

/**
 * Created by jitrapon
 */
public class CheckedItem {

    private BoardItem item;
    private int state;

    public CheckedItem(int state, BoardItem item) {
        this.item = item;
        this.state = state;
    }

    public BoardItem getItem() { return item; }

    public int getState() { return state; }

    public void setState(int state) {
        this.state = state;
    }
}
