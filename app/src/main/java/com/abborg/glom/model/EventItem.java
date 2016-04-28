package com.abborg.glom.model;

import android.location.Location;

import org.joda.time.DateTime;

/**
 * Base class for all event types. An event encapsulates basic information about its host, time of event,
 * and optionally location of the event
 *
 * Layout file: event_card.xml
 *
 * Created by Boat on 11/10/58.
 */
public class EventItem extends BoardItem {

    /* The display name of the event - REQUIRED */
    private String name;

    /* OPTIONAL start time */
    private DateTime startTime;

    /* OPTIONAL ending time of the event */
    private DateTime endTime;

    /* Google's provided place ID - OPTIONAL */
    private String place;

    /* Location of the event - OPTIONAL */
    private Location location;

    /* Notes about this event - OPTIONAL */
    private String note;

    private EventItem() {}

    public static EventItem createEvent(Circle circle, DateTime createdTime, DateTime updatedTime) {
        return createEvent(generateId(), circle, createdTime, updatedTime);
    }

    public static EventItem createEvent(String id, Circle circle, DateTime createdTime, DateTime updatedTime) {
        EventItem event = new EventItem();
        event.id = id;
        event.type = TYPE_EVENT;
        event.circle = circle;
        event.createdTime = createdTime;
        event.updatedTime = updatedTime;
        return event;
    }

    public void setEventInfo(String name, DateTime startTime, DateTime endTime, String placeId,
                             Location location, String note) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.place = placeId;
        this.location = location;
        this.note = note;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle c) {
        circle = c;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public String getPlace() { return place; }

    public void setPlace(String place) { this.place = place; }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setEndTime(DateTime endTime) { this.endTime = endTime; }

    public DateTime getEndTime() { return endTime; }
}
