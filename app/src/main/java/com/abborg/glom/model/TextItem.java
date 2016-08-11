package com.abborg.glom.model;

import org.joda.time.DateTime;

/**
 * Simple class to model a text in the ListItem
 *
 * Created by jitrapon
 */
public class TextItem extends BoardItem {

    private String text;

    public static TextItem createText(Circle c) {
        return new TextItem(generateId(), c);
    }

    public static TextItem createText(String id, Circle circle, DateTime created, DateTime updated) {
        TextItem item = new TextItem(id, circle);
        item.setCreatedTime(created);
        item.setUpdatedTime(updated);
        return item;
    }

    private TextItem(String textId, Circle c) {
        id = textId;
        type = BoardItem.TYPE_TEXT;
        circle = c;
        createdTime = DateTime.now();
        updatedTime = DateTime.now();
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() { return text; }
}
