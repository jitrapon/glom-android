package com.abborg.glom.model;

import org.joda.time.DateTime;

import java.util.List;

public class MovieItem extends ExploreItem {

    private String title;
    private String summary;
    private String genre;
    private String lang;
    private DateTime releaseDate;
    private String director;
    private String cast;
    private List<WatchableRating> ratings;
    private List<WatchableImage> images;
    private List<WatchableVideo> videos;
    private List<WatchableFeed> feeds;

    public MovieItem() { super(); }

    public MovieItem setId(String id) {
        this.id = id;
        return this;
    }

    public MovieItem setCreatedTime(DateTime createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    public MovieItem setUpdatedTime(DateTime updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }

    public MovieItem setTitle(String title) {
        this.title = title;
        return this;
    }

    public MovieItem setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public MovieItem setGenre(String genre) {
        this.genre = genre;
        return this;
    }

    public MovieItem setLang(String lang) {
        this.lang = lang;
        return this;
    }

    public MovieItem setReleaseDate(DateTime time) {
        this.releaseDate = time;
        return this;
    }

    public MovieItem setDirector(String director) {
        this.director = director;
        return this;
    }

    public MovieItem setCast(String cast) {
        this.cast = cast;
        return this;
    }

    public MovieItem setRatings(List<WatchableRating> ratings) {
        this.ratings = ratings;
        return this;
    }

    public MovieItem setImages(List<WatchableImage> images) {
        this.images = images;
        return this;
    }

    public MovieItem setVideos(List<WatchableVideo> videos) {
        this.videos = videos;
        return this;
    }

    public MovieItem setFeeds(List<WatchableFeed> feeds) {
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
        return ExploreItem.TYPE_MOVIE;
    }
}
