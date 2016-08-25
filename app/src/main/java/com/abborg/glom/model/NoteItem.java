package com.abborg.glom.model;

import org.joda.time.DateTime;

/**
 * Created by jitrapon
 */
public class NoteItem extends BoardItem {

    private String title;

    private String text;

    public static NoteItem createNote(Circle circle) {
        return new NoteItem(generateId(), circle);
    }

    public static NoteItem createNote(String id, Circle circle, DateTime created, DateTime updated) {
        NoteItem item = new NoteItem(id, circle);
        item.setCreatedTime(created);
        item.setUpdatedTime(updated);
        return item;
    }

    private NoteItem(String id, Circle c) {
        this.id = id;
        type = BoardItem.TYPE_NOTE;
        circle = c;
        createdTime = DateTime.now();
        updatedTime = DateTime.now();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() { return title; }

    public String getText() { return text; }
}
