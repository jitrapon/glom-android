package com.abborg.glom.model;

public class PlaceInfo {

    private CharSequence name;
    private double lat;
    private double lng;

    public PlaceInfo(CharSequence name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public CharSequence getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
}
