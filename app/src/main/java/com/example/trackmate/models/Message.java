package com.example.trackmate.models;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String senderId;
    private String receiverId;
    private String text;
    private String imageUrl;
    private long timestamp;
    private String receiverName;
    private String receiverImage;
    private String receiverEmail; // Email of the receiver for fallback name
    private String relatedItemId; // ID of the related item (if any)
    private String itemImageUrl; // Image URL of the related item
    private String itemName; // Name of the related item

    // Default constructor for Firebase
    public Message() {
    }
    
    // Constructor with required fields
    public Message(String senderId, String receiverId, String text) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter and Setter for senderId
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    // Getter and Setter for receiverId
    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    // Getter and Setter for text
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    // Getter and Setter for imageUrl
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Getter and Setter for timestamp
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Getter and Setter for receiverName
    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    // Getter and Setter for receiverImage
    public String getReceiverImage() {
        return receiverImage;
    }

    public void setReceiverImage(String receiverImage) {
        this.receiverImage = receiverImage;
    }
    
    // Getter and Setter for receiverEmail
    public String getReceiverEmail() {
        return receiverEmail;
    }

    public void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
    }
    
    // Getter and Setter for relatedItemId
    public String getRelatedItemId() {
        return relatedItemId;
    }

    public void setRelatedItemId(String relatedItemId) {
        this.relatedItemId = relatedItemId;
    }
    
    // Getter and Setter for itemImageUrl
    public String getItemImageUrl() {
        return itemImageUrl;
    }

    public void setItemImageUrl(String itemImageUrl) {
        this.itemImageUrl = itemImageUrl;
    }
    
    // Getter and Setter for itemName
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("senderId", senderId);
        result.put("receiverId", receiverId);
        result.put("text", text);
        result.put("imageUrl", imageUrl);
        result.put("timestamp", timestamp);
        result.put("receiverName", receiverName);
        result.put("receiverImage", receiverImage);
        result.put("receiverEmail", receiverEmail);
        result.put("relatedItemId", relatedItemId);
        result.put("itemImageUrl", itemImageUrl);
        result.put("itemName", itemName);
        return result;
    }
}
