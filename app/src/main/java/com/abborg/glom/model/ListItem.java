package com.abborg.glom.model;

import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to model a list (to-do) item. Each item is a text item that
 * holds a checked state.
 *
 * Created by jitrapon
 */
public class ListItem extends BoardItem {

    public static final int STATE_DEFAULT = 0;    // default state of the item
    public static final int STATE_CHECKED = 1;    // checked state of the item

    private static final String TAG = "ListItem";

    private String title;

    /* Contains list of mapping between item states and items */
    private List<CheckedItem> items;

    public static ListItem createList(Circle circle) {
        return new ListItem(generateId(), circle);
    }

    public static ListItem createList(String id, Circle circle, DateTime created, DateTime updated) {
        ListItem item = new ListItem(id, circle);
        item.setCreatedTime(created);
        item.setUpdatedTime(updated);
        return item;
    }

    private ListItem(String listId, Circle c) {
        id = listId;
        type = BoardItem.TYPE_LIST;
        circle = c;
        createdTime = DateTime.now();
        updatedTime = DateTime.now();
        items = new ArrayList<>();
    }

    public void setTitle(String title) { this.title = title; }

    public String getTitle() { return title; }

    public void setItems(List<CheckedItem> items) {
        this.items = items;
    }

    public CheckedItem getItem(int index) {
        try {
            return items.get(index);
        }
        catch(Exception ex) {
            Log.e(TAG, ex.getMessage());
        }

        return null;
    }

    public List<CheckedItem> getItems() { return items; }

    public void addItem(CheckedItem item) {
        items.add(item);
    }
}
