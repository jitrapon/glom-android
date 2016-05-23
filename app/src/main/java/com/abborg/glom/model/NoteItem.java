package com.abborg.glom.model;

import org.joda.time.DateTime;

/**
 * Created by jitrapon on 23/5/16.
 */
public class NoteItem extends BoardItem {

    private String name;

    private NoteItem() {}

    public static NoteItem createNote(Circle circle, DateTime createdTime, DateTime updatedTime) {
        return createNote(generateId(), circle, createdTime, updatedTime);
    }

    public static NoteItem createNote(String id, Circle circle, DateTime createdTime, DateTime updatedTime) {
        NoteItem note = new NoteItem();
        note.type = TYPE_NOTE;
        note.id = id;
        note.circle = circle;
        note.createdTime = createdTime;
        note.updatedTime = updatedTime;
        return note;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
}
