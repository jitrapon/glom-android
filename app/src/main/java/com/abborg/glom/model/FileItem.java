package com.abborg.glom.model;

import android.text.TextUtils;

import com.abborg.glom.Const;
import com.abborg.glom.utils.FileUtils;

import org.joda.time.DateTime;

import java.io.File;

/**
 * Class to model a file.
 *
 * Layout file: file_card.xml
 */
public class FileItem extends BoardItem {

    /* Fie name */
    private String name;

    /* File size */
    private long size;

    /* Note about this file */
    private String note;

    /* File mimetype */
    private String mimetype;

    /* Reference to file */
    private File file;

    public static FileItem createFile(Circle circle) {
        return new FileItem(generateId(), circle, null);
    }

    public static FileItem createFile(String id, Circle circle, String path, DateTime created, DateTime updated) {
        FileItem item = new FileItem(id, circle, path);
        item.setCreatedTime(created);
        item.setUpdatedTime(updated);
        return item;
    }

    private FileItem(String fileId, Circle c, String path) {
        id = fileId;
        type = BoardItem.TYPE_FILE;
        circle = c;
        createdTime = DateTime.now();
        updatedTime = DateTime.now();
        file = TextUtils.isEmpty(path) ? null : new File(path);
    }

    public void setPath(String path) {
        file = new File(path);
    }

    public File getLocalFile() {
        return file;
    }

    public boolean isGif() {
        return !TextUtils.isEmpty(mimetype) && (mimetype.equals(Const.FILE_TYPE_GIF));
    }

    public boolean isImage() {
        return FileUtils.isImage(mimetype);
    }

    public boolean isVideo() {
        return FileUtils.isVideo(mimetype);
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getName() { return name; }

    public long getSize() { return size; }

    public String getNote() { return note; }

    public String getMimetype() { return mimetype; }

    public void setName(String name) { this.name = name; }

    public void setSize(long size) { this.size = size; }
}
