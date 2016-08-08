package com.abborg.glom.model;

import org.joda.time.DateTime;

/**
 * Class to model a link.
 *
 * Created by jitrapon
 */
public class LinkItem extends BoardItem {

    private String url;

    private String thumbnail;

    private String title;

    private String description;

    private int maxFetchPages;

    private int maxLinkDepth;

    public static LinkItem createLink(Circle circle) {
        return new LinkItem(generateId(), circle);
    }

    public static LinkItem createLink(String id, Circle circle, DateTime created, DateTime updated) {
        LinkItem item = new LinkItem(id, circle);
        item.setCreatedTime(created);
        item.setUpdatedTime(updated);
        return item;
    }

    private LinkItem(String linkId, Circle c) {
        id = linkId;
        type = BoardItem.TYPE_LINK;
        circle = c;
        createdTime = DateTime.now();
        updatedTime = DateTime.now();
        maxFetchPages = -1;
        maxLinkDepth = 0;
    }

    public void setLinkInfo(String url, String thumbnail, String title, String description) {
        this.url = url;
        this.thumbnail = thumbnail;
        this.title = title;
        this.description = description;
    }

    public String getUrl() { return url; }

    public void setUrl(String url) { this.url = url; }

    public String getThumbnail() { return thumbnail; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public int getMaxFetchPages() { return maxFetchPages; }

    public int getMaxLinkDepth() { return maxLinkDepth; }
}
