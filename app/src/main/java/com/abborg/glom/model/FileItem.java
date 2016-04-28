package com.abborg.glom.model;

import android.net.Uri;
import android.text.TextUtils;

import com.abborg.glom.Const;

import org.joda.time.DateTime;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

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

    /* File uri in the local system */
    private Uri uri;

    /* File mimetype */
    private String mimetype;

    /* Reference to file */
    private File file;

    private static List<String> imageFileTypes = Arrays.asList(
            Const.FILE_TYPE_JPEG,
            Const.FILE_TYPE_JPG,
            Const.FILE_TYPE_GIF,
            Const.FILE_TYPE_PNG,
            Const.FILE_TYPE_BMP,
            Const.FILE_TYPE_WBMP,
            Const.FILE_TYPE_WEBP
    );

    public static FileItem createFile(Circle circle, Uri uri) throws URISyntaxException {
        return new FileItem(generateId(), circle, uri);
    }

    private FileItem(String fileId, Circle c, Uri fileUri) throws URISyntaxException {
        id = fileId;
        type = BoardItem.TYPE_FILE;
        circle = c;
        createdTime = DateTime.now();
        updatedTime = DateTime.now();
        uri = fileUri;
    }

    public void setPath(String path) {
        file = new File(path);
    }

    public File getFile() {
        return file;
    }

    public boolean isGif() {
        return !TextUtils.isEmpty(mimetype) && (mimetype.equals(Const.FILE_TYPE_GIF));
    }

    public boolean isImage() {
        return !TextUtils.isEmpty(mimetype) && imageFileTypes.contains(mimetype);
    }

    public String getUri() { return uri==null ? "" : uri.getPath(); }

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
