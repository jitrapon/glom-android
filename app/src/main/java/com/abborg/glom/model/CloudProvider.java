package com.abborg.glom.model;

public enum CloudProvider {
    AMAZON_S3(0),
    DROPBOX(1),
    GOOGLE_DRIVE(2);

    private final int id;
    CloudProvider (int id) { this.id = id; }
    public int getId() { return id; }
}