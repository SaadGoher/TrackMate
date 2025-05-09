package com.example.trackmate.utils;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for performance monitoring and debugging in the TrackMate app.
 * This helps track and log long-running operations to identify performance bottlenecks.
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static final Map<String, Long> operationStartTimes = new HashMap<>();
    private static final Map<String, Long> thresholds = new HashMap<>();
    
    // Warning thresholds for different operations (in milliseconds)
    static {
        // Define default thresholds
        thresholds.put("image_processing", 500L);
        thresholds.put("firebase_query", 1000L);
        thresholds.put("similarity_calculation", 200L);
        thresholds.put("embedding_generation", 1000L);
        thresholds.put("ui_update", 100L);
    }
    
    /**
     * Start timing an operation
     * 
     * @param operationId Unique identifier for the operation
     * @param description Description of the operation
     */
    public static void startOperation(String operationId, String description) {
        operationStartTimes.put(operationId, System.currentTimeMillis());
        Log.d(TAG, "⏱️ Starting operation: " + description + " [" + operationId + "]");
    }
    
    /**
     * End timing an operation and log the result
     * 
     * @param operationId Unique identifier for the operation
     * @param description Description of the operation
     * @param category Category of operation for threshold checking
     */
    public static void endOperation(String operationId, String description, String category) {
        if (operationStartTimes.containsKey(operationId)) {
            long startTime = operationStartTimes.get(operationId);
            long duration = System.currentTimeMillis() - startTime;
            
            // Get threshold for this category of operation
            long threshold = thresholds.getOrDefault(category, 500L);
            
            if (duration > threshold) {
                // Operation took longer than the threshold
                Log.w(TAG, "⚠️ SLOW OPERATION: " + description + " took " + duration + 
                      "ms [" + operationId + "]");
            } else {
                Log.d(TAG, "✅ Completed: " + description + " in " + duration + 
                      "ms [" + operationId + "]");
            }
            
            // Remove from map to avoid memory leaks
            operationStartTimes.remove(operationId);
        } else {
            Log.w(TAG, "❌ Attempted to end timing for unknown operation: " + 
                  operationId + " - " + description);
        }
    }
    
    /**
     * Record a single measurement without start/end tracking
     * 
     * @param description Description of what was measured
     * @param durationMs Duration in milliseconds
     * @param category Category for threshold checking
     */
    public static void recordMeasurement(String description, long durationMs, String category) {
        long threshold = thresholds.getOrDefault(category, 500L);
        
        if (durationMs > threshold) {
            Log.w(TAG, "⚠️ SLOW OPERATION: " + description + " took " + durationMs + "ms");
        } else {
            Log.d(TAG, "📊 Measurement: " + description + " - " + durationMs + "ms");
        }
    }
    
    /**
     * Set a custom threshold for a category of operations
     * 
     * @param category The operation category
     * @param thresholdMs Threshold in milliseconds
     */
    public static void setThreshold(String category, long thresholdMs) {
        thresholds.put(category, thresholdMs);
    }
}
