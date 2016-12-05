package com.abborg.glom.model;

import org.joda.time.DateTime;

import java.io.File;

/**
 * Created by jitrapon
 */
public class DrawItem extends BoardItem {

    private String name;
    private File file;

    private DrawItem() {}

    public static DrawItem createDrawing(Circle circle, DateTime createdTime, DateTime updatedTime) {
        return createDrawing(generateId(), circle, createdTime, updatedTime);
    }

    public static DrawItem createDrawing(String id, Circle circle, DateTime createdTime, DateTime updatedTime) {
        DrawItem drawItem = new DrawItem();
        drawItem.type = TYPE_DRAWING;
        drawItem.id = id;
        drawItem.circle = circle;
        drawItem.createdTime = createdTime;
        drawItem.updatedTime = updatedTime;
        return drawItem;
    }

    public void setPath(String path) {
        file = new File(path);
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public File getLocalFile() { return file; }
}
