package com.example.trackmate.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class representing an image embedding for a reported item.
 */
public class ImageEmbedding {
    private String itemId;
    private float[] embedding;
    private long timestamp;

    public ImageEmbedding() {
        // Required empty constructor for Firebase
    }

    public ImageEmbedding(String itemId, float[] embedding) {
        this.itemId = itemId;
        this.embedding = embedding;
        this.timestamp = System.currentTimeMillis();
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Convert float array to List of Floats for Firebase storage
     */
    private List<Float> embeddingToList() {
        if (embedding == null) {
            return new ArrayList<>();
        }
        
        List<Float> list = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            list.add(value);
        }
        return list;
    }

    /**
     * Convert to a Map for Firebase storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", itemId);
        map.put("embedding", embeddingToList()); // Store as List instead of array
        map.put("timestamp", timestamp);
        return map;
    }

    @Override
    public String toString() {
        return "ImageEmbedding{" +
                "itemId='" + itemId + '\'' +
                ", embedding=" + (embedding != null ? Arrays.toString(embedding) : "null") +
                ", timestamp=" + timestamp +
                '}';
    }
}

