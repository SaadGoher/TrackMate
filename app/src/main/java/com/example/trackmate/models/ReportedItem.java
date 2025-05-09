package com.example.trackmate.models;

public class ReportedItem {
    public enum Status {
        OPEN, CLOSED, RESOLVED
    }

    public enum Type {
        LOST, FOUND
    }

    private String id;
    private String userId;
    private String name;
    private String description;
    private String location;
    private String date;
    private String imageUrl;
    private Type type;
    private Status status;
    private long timestamp;
    private String receiverId;
    private double latitude;
    private double longitude;
    private boolean useCurrentLocation;
    // Adding a string representation of type for Firebase serialization
    private String typeString;
    // Adding a string representation of status for Firebase serialization
    private String statusString;
    // Default constructor for Firebase
    public ReportedItem() {
    }

    // Constructor with parameters
    public ReportedItem(String name, String description, String location,
                        String date, String imageUrl, Type type) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.date = date;
        this.imageUrl = imageUrl;
        this.type = type;
        this.typeString = type.toString(); // Set the String version
        this.status = Status.OPEN;
        this.timestamp = System.currentTimeMillis();
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.useCurrentLocation = false;
    }

    // Constructor with location parameters
    public ReportedItem(String name, String description, String location,
                        String date, String imageUrl, Type type,
                        double latitude, double longitude, boolean useCurrentLocation) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.date = date;
        this.imageUrl = imageUrl;
        this.type = type;
        this.typeString = type.toString(); // Set the String version
        this.status = Status.OPEN;
        this.timestamp = System.currentTimeMillis();
        this.latitude = latitude;
        this.longitude = longitude;
        this.useCurrentLocation = useCurrentLocation;
    }


    public String getId() {
        return id;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
        this.typeString = type.toString();
    }

    // Getter and setter for typeString (used by Firebase)
    public String getTypeString() {
        return typeString;
    }

    public void setTypeString(String typeString) {
        this.typeString = typeString;
        // Also set the enum value if possible
        try {
            this.type = Type.valueOf(typeString);
        } catch (Exception e) {
            // Ignore invalid values
        }
    }

    public boolean isLost() {
        return Type.LOST.equals(type);
    }

    public void setId(String key) {
        this.id = key;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.statusString = status != null ? status.toString() : null;
    }
    
    // Getter and setter for statusString (used by Firebase)
    public String getStatusString() {
        return statusString;
    }
    
    public void setStatusString(String statusString) {
        this.statusString = statusString;
        // Also set the enum value if possible
        try {
            if (statusString != null) {
                this.status = Status.valueOf(statusString);
            }
        } catch (Exception e) {
            // Default to OPEN if invalid
            this.status = Status.OPEN;
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isUseCurrentLocation() {
        return useCurrentLocation;
    }

    public void setUseCurrentLocation(boolean useCurrentLocation) {
        this.useCurrentLocation = useCurrentLocation;
    }
}