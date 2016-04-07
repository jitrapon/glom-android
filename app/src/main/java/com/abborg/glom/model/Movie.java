package com.abborg.glom.model;

import org.joda.time.DateTime;

import java.util.List;

public class Movie extends DiscoverItem {

    String title;
    String summary;
    String genre;
    String lang;
    DateTime releaseDate;
    String director;
    String cast;
    List<WatchableRating> ratings;
    List<WatchableImage> images;
    List<WatchableVideo> videos;
    List<WatchableFeed> feeds;

    public Movie() { super(); }

    public Movie setId(String id) {
        this.id = id;
        return this;
    }

    public Movie setCreatedTime(DateTime createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    public Movie setUpdatedTime(DateTime updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }

    public Movie setTitle(String title) {
        this.title = title;
        return this;
    }

    public Movie setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public Movie setGenre(String genre) {
        this.genre = genre;
        return this;
    }

    public Movie setLang(String lang) {
        this.lang = lang;
        return this;
    }

    public Movie setReleaseDate(DateTime time) {
        this.releaseDate = time;
        return this;
    }

    public Movie setDirector(String director) {
        this.director = director;
        return this;
    }

    public Movie setCast(String cast) {
        this.cast = cast;
        return this;
    }

    public Movie setRatings(List<WatchableRating> ratings) {
        this.ratings = ratings;
        return this;
    }

    public Movie setImages(List<WatchableImage> images) {
        this.images = images;
        return this;
    }

    public Movie setVideos(List<WatchableVideo> videos) {
        this.videos = videos;
        return this;
    }

    public Movie setFeeds(List<WatchableFeed> feeds) {
        this.feeds = feeds;
        return this;
    }

    public String getTitle() { return title; }

    public String getSummary() { return summary; }

    public String getGenre() { return genre; }

    public String getLang() { return lang; }

    public DateTime getReleaseDate() { return releaseDate; }

    public String getDirector() { return director; }

    public String getCast() { return cast; }

    public List<WatchableRating> getRatings() { return ratings; }

    public List<WatchableImage> getImages() { return images; }

    public List<WatchableVideo> getVideos() { return videos; }

    public List<WatchableFeed> getFeeds() { return feeds; }

    @Override
    public int getType() {
        return DiscoverItem.TYPE_MOVIE;
    }
}
