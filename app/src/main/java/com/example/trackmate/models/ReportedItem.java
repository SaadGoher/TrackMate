package com.example.trackmate.models;

public class ReportedItem {
    private String name;
    private String date;
    private String location;
    private String description;
    private String imageUrl;
    private String userId;
    private boolean lost;

    public ReportedItem() {
        // Default constructor required for calls to DataSnapshot.getValue(ReportedItem.class)
    }

    public ReportedItem(String name, String date, String location, String description, String imageUrl) {
        this.name = name;
        this.date = date;
        this.location = location;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isLost() {
        return lost;
    }

    public void setLost(boolean lost) {
        this.lost = lost;
    }
}