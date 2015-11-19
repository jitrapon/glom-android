package com.abborg.glom.model;

import android.location.Location;

import org.joda.time.DateTime;

import java.util.List;
import java.util.UUID;

/**
 * Base class for all event types. An event encapsulates basic information about its host, time of event,
 * and optionally location of the event
 *
 * Layout file: event_card.xml
 *
 * Created by Boat on 11/10/58.
 */
public class Event {

    /* The display name of the event - REQUIRED */
    private String name;

    /* circle associated with this event */
    private Circle circle;

    /* unique ID of the event */
    private String id;

    /* The host of the event, can be entities as well. There can be more than one
     * hosts for an event - REQUIRED */
    private List<User> hosts;

    /* Date and time of the event - OPTIONAL */
    private DateTime dateTime;

    /* Google's provided place ID - OPTIONAL */
    private String place;

    /* Location of the event - OPTIONAL */
    private Location location;

    /* List of invitees
    * If the discoverType is IN_CIRCLE, the list will be the users in the circle
    * If the discoverType is ALL_FRINEDS, the list will be all the users' friends
    * If the discoverType is INVITE_ONLY, the list will contains list of invitees
    * If the discoverType is PUBLIC, the list will always be empty, and not expected to be used in another context
    * */
    private List<User> invitees;

    /**
     * List of attendees who accept to attend this event
     */
    private List<User> attendees;

    /* Whether or not event is reoccuring */
    /* Reoccuring events will be created automatically */
    private boolean reoccuring;

    /* Show hosts to the users who can view the event (default is true) */
    private boolean showHosts;

    /* Show invitees to the users who can view the event (default is true) */
    private boolean showInvitees;

    /* Show the attendees to the users who can view the event (default is true) */
    private boolean showAttendees;

    /* How the event will be viewed to others (default is IN_CIRCLE) */
    private int discoverType;

    /* Notes about this event - OPTIONAL */
    private String note;

    /* OPTIONAL ending time of the event */
    private DateTime endTime;

    /* The last user action to the event for use as display */
    private FeedAction lastAction;

    /* Default value; only users in the circle can see the event */
    public static final int IN_CIRCLE = 1;

    /* All users in your friend list can see the event */
    public static final int ALL_FRIENDS = 2;

    /* Only the invitees can see the event */
    public static final int INVITE_ONLY = 3;

    /* Public events. */
    public static final int PUBLIC = 4;

    /**
     * Create events with default values and some basic information
     * */
    public static Event createEvent(String id, Circle circle, String name, DateTime dateTime, String place,
                                    Location location, String note) {
        Event event = createEvent(name, circle, null, dateTime, place, location, Event.IN_CIRCLE, null,
                true, true, true, note);
        event.setId(id);
        return event;
    }

    private Event() {}

    /**
     * Creates a new event from the circle. An event cannot exist without a created Circle unless
     * its discoverType is PUBLIC.
     *
     * @param name
     * @param circle
     * @param hosts
     * @param dateTime OPTIONAL
     * @param location OPTIONAL
     * @param discoverType
     * @return
     */
    public static Event createEvent(String name, Circle circle, List<User> hosts, DateTime dateTime, String place,
                                    Location location, int discoverType, List<User> invitees,
                                    boolean showHosts, boolean showInvitees, boolean showAttendees, String note) {
        String id = generateEventId();

        //TODO add to SQLITE and update server
        return new Event(name, id, circle, hosts, dateTime, place, location, discoverType, invitees, showHosts,
                showInvitees, showAttendees, note);
    }

    private Event(String name, String id, Circle circle, List<User> hosts, DateTime dateTime, String place, Location location,
                  int discoverType, List<User> invitees,
                  boolean showHosts, boolean showInvitees, boolean showAttendees, String note) {
        this.name = name;
        this.id = id;
        this.circle = circle;
        this.hosts = hosts;
        this.dateTime = dateTime;
        this.endTime = null;
        this.place = place;
        this.location = location;
        this.discoverType = discoverType;
        this.invitees = invitees;
        this.showHosts = showHosts;
        this.showInvitees = showInvitees;
        this.showAttendees = showAttendees;
        this.note = note;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle c) {
        circle = c;
    }

    public List<User> getHosts() {
        return hosts;
    }

    public void setHosts(List<User> hosts) {
        this.hosts = hosts;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getPlace() { return place; }

    public void setPlace(String place) { this.place = place; }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getDiscoverType() {
        return discoverType;
    }

    public void setDiscoverType(int discoverType) {
        this.discoverType = discoverType;
    }

    public List<User> getInvitees() {
        return invitees;
    }

    public void addInvitees(List<User> newUsers) {
        this.invitees.addAll(newUsers);
    }

    public void setInvitees(List<User> invitees) {
        this.invitees = invitees;
    }

    public List<User> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<User> attendees) {
        this.attendees = attendees;
    }

    public boolean isHostShown() {
        return showHosts;
    }

    public void setHostsShown(boolean enable) {
        showHosts = enable;
    }

    public boolean isInviteeShown() {
        return showInvitees;
    }

    public void setInviteesShown(boolean enable) {
        showInvitees = enable;
    }

    public boolean isAttendeeShown() {
        return showAttendees;
    }

    public void setAttendeesShown(boolean enable) {
        showAttendees = enable;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isReoccuring() { return reoccuring; }

    public void setReoccuring(boolean enable) { reoccuring = enable; }

    public FeedAction getLastAction() { return lastAction; }

    public void setLastAction(FeedAction action) {
        lastAction = action;
    }

    public void setEndTime(DateTime endTime) { this.endTime = endTime; }

    public DateTime getEndTime() { return endTime; }

    private static String generateEventId() {
        return String.valueOf(UUID.randomUUID());
    }
}
