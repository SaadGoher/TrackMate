package com.example.trackmate.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.trackmate.models.ImageEmbedding;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.services.FirebaseService;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Helper class for item matching functionality
 */
public class ItemMatcher {
    private static final String TAG = "ItemMatcher";
    private static final float SIMILARITY_THRESHOLD = 0.70f; // Threshold for determining a potential match
    private static final float TEXT_MATCH_WEIGHT = 0.3f; // Weight for text matching (name, description, location)
    private static final float IMAGE_MATCH_WEIGHT = 0.7f; // Weight for image similarity

    /**
     * Process a new reported item, generate embedding, and find potential matches
     * 
     * @param context The application context
     * @param item The reported item
     * @param imageBitmap The item's image
     * @param listener Callback for match results
     */
    public static void processNewItem(Context context, ReportedItem item, Bitmap imageBitmap, MatchListener listener) {
        Log.d(TAG, "🔍 Starting to process new item: " + item.getId() + " - " + item.getName());
        try {
            // Generate embedding for the new item
            Log.d(TAG, "🖼️ Generating image embedding for item: " + item.getId());
            long startTime = System.currentTimeMillis();
            ImageSimilaritySearch searcher = ImageSimilaritySearch.getInstance(context);
            float[] embedding = searcher.generateEmbedding(imageBitmap);
            long embedTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "⏱️ Image embedding generation took: " + embedTime + "ms");
            
            if (embedding == null) {
                Log.e(TAG, "❌ Failed to generate embedding for item: " + item.getId());
                listener.onError("Failed to generate image embedding");
                return;
            }
            
            // Save embedding to Firebase
            Log.d(TAG, "💾 Saving embedding to Firebase for item: " + item.getId());
            ImageEmbedding imageEmbedding = new ImageEmbedding(item.getId(), embedding);
            FirebaseService.saveImageEmbedding(imageEmbedding, task -> {
                try {
                    if (!task.isSuccessful()) {
                        String errorMsg = "Failed to save image embedding";
                        if (task.getException() != null) {
                            errorMsg += ": " + task.getException().getMessage();
                            Log.e(TAG, "❌ " + errorMsg, task.getException());
                        }
                        listener.onError(errorMsg);
                        return;
                    }
                    
                    Log.d(TAG, "✅ Successfully saved embedding for item: " + item.getId());
                    
                    // Find potential matches based on item type
                    ReportedItem.Type oppositeType = (item.getType() == ReportedItem.Type.LOST) 
                            ? ReportedItem.Type.FOUND 
                            : ReportedItem.Type.LOST;
                    
                    Log.d(TAG, "🔍 Finding potential matches of type: " + oppositeType + " for item: " + item.getId());
                    findPotentialMatches(context, imageEmbedding, oppositeType, listener);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error processing item after saving embedding", e);
                    listener.onError("Error processing item: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error processing new item", e);
            listener.onError("Error processing item: " + e.getMessage());
        }
    }
    
    /**
     * Find potential matches for an item
     * 
     * @param context The application context
     * @param newEmbedding The embedding for the new item
     * @param typeToMatch The type of items to match against (opposite of the new item's type)
     * @param listener Callback for match results
     */
    private static void findPotentialMatches(Context context, ImageEmbedding newEmbedding, 
                                             ReportedItem.Type typeToMatch, MatchListener listener) {
        try {
            // First get the item details for text-based matching
            FirebaseService.getDatabase().child("reported_items").child(newEmbedding.getItemId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            ReportedItem newItem = snapshot.getValue(ReportedItem.class);
                            if (newItem == null) {
                                listener.onError("Could not find item details");
                                return;
                            }
                            newItem.setId(newEmbedding.getItemId());
                            
                            // Get all items of the opposite type for text matching
                            FirebaseService.getDatabase().child("reported_items")
                                    .orderByChild("typeString")
                                    .equalTo(typeToMatch.toString())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            List<ReportedItem> oppositeItems = new ArrayList<>();
                                            for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                                                ReportedItem item = itemSnapshot.getValue(ReportedItem.class);
                                                if (item != null) {
                                                    item.setId(itemSnapshot.getKey());
                                                    oppositeItems.add(item);
                                                }
                                            }
                                            
                                            if (oppositeItems.isEmpty()) {
                                                listener.onNoMatchesFound();
                                                return;
                                            }
                                            
                                            // Now get the image embeddings for these items
                                            FirebaseService.getImageEmbeddingsByType(typeToMatch, new FirebaseService.EmbeddingsCallback() {
                                                @Override
                                                public void onEmbeddingsLoaded(List<ImageEmbedding> embeddings) {
                                                    try {
                                                        // Map embeddings by item ID for easy access
                                                        Map<String, float[]> imageEmbeddings = new HashMap<>();
                                                        for (ImageEmbedding embedding : embeddings) {
                                                            imageEmbeddings.put(embedding.getItemId(), embedding.getEmbedding());
                                                        }
                                                        
                                                        // Find matches using both text and image similarity
                                                        List<MatchResult> matches = findMatches(
                                                                context, 
                                                                newItem, 
                                                                oppositeItems, 
                                                                imageEmbeddings, 
                                                                newEmbedding.getEmbedding());
                                                        
                                                        if (matches.isEmpty()) {
                                                            listener.onNoMatchesFound();
                                                        } else {
                                                            // Retrieve the full item details for the matches
                                                            retrieveMatchDetails(matches, listener);
                                                        }
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "Error processing embeddings", e);
                                                        listener.onError("Error processing potential matches: " + e.getMessage());
                                                    }
                                                }
                                                
                                                @Override
                                                public void onError(String errorMessage) {
                                                    // Fall back to just text matching if image embeddings fail
                                                    try {
                                                        Log.w(TAG, "Image embedding failed, falling back to text matching: " + errorMessage);
                                                        
                                                        // Do text-only matching
                                                        List<MatchResult> textMatches = new ArrayList<>();
                                                        for (ReportedItem item : oppositeItems) {
                                                            float textSimilarity = calculateTextSimilarity(newItem, item);
                                                            if (textSimilarity >= 0.4f) { // Higher threshold for text-only
                                                                textMatches.add(new MatchResult(item.getId(), textSimilarity));
                                                            }
                                                        }
                                                        
                                                        // Sort matches
                                                        Collections.sort(textMatches, (a, b) -> 
                                                                Float.compare(b.getSimilarityScore(), a.getSimilarityScore()));
                                                        
                                                        if (textMatches.isEmpty()) {
                                                            listener.onNoMatchesFound();
                                                        } else {
                                                            retrieveMatchDetails(textMatches, listener);
                                                        }
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "Error with text matching fallback", e);
                                                        listener.onError("Error finding matches: " + errorMessage);
                                                    }
                                                }
                                            });
                                        }
                                        
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e(TAG, "Error retrieving items: " + error.getMessage());
                                            listener.onError("Error retrieving items: " + error.getMessage());
                                        }
                                    });
                        }
                        
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error retrieving item details: " + error.getMessage());
                            listener.onError("Error retrieving item details: " + error.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error finding potential matches", e);
            listener.onError("Error finding potential matches: " + e.getMessage());
        }
    }
    
    /**
     * Retrieve full item details for the matches
     * 
     * @param matchResults The list of potential matches
     * @param listener Callback for match results
     */
    private static void retrieveMatchDetails(List<MatchResult> matchResults, MatchListener listener) {
        try {
            List<ReportedItem> matchedItems = new ArrayList<>();
            Map<String, Float> similarityScores = new HashMap<>();
            
            // Store similarity scores for each item
            for (MatchResult result : matchResults) {
                similarityScores.put(result.getItemId(), result.getSimilarityScore());
            }
            
            // Retrieve item details one by one
            retrieveNextItem(0, matchResults, matchedItems, similarityScores, listener);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving match details", e);
            listener.onError("Error retrieving item details: " + e.getMessage());
        }
    }
    
    /**
     * Recursively retrieve item details
     */
    private static void retrieveNextItem(int index, List<MatchResult> matchResults, 
                                        List<ReportedItem> matchedItems, 
                                        Map<String, Float> similarityScores, 
                                        MatchListener listener) {
        try {
            if (index >= matchResults.size()) {
                // All items retrieved
                listener.onMatchesFound(matchedItems, similarityScores);
                return;
            }
            
            String itemId = matchResults.get(index).getItemId();
            
            FirebaseService.getDatabase()
                    .child("reported_items")
                    .child(itemId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            try {
                                if (snapshot.exists()) {
                                    ReportedItem item = snapshot.getValue(ReportedItem.class);
                                    if (item != null) {
                                        item.setId(itemId); // Make sure ID is set
                                        matchedItems.add(item);
                                    }
                                }
                                
                                // Process the next item
                                retrieveNextItem(index + 1, matchResults, matchedItems, similarityScores, listener);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing item " + itemId, e);
                                // Continue with next item
                                retrieveNextItem(index + 1, matchResults, matchedItems, similarityScores, listener);
                            }
                        }
                        
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Firebase error for item " + itemId + ": " + error.getMessage(), error.toException());
                            // Continue with the next item even if one fails
                            retrieveNextItem(index + 1, matchResults, matchedItems, similarityScores, listener);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving next item at index " + index, e);
            // Try to continue with the next item
            retrieveNextItem(index + 1, matchResults, matchedItems, similarityScores, listener);
        }
    }
    
    /**
     * Calculate text similarity score between two items based on name, description and location
     * 
     * @param item1 First item
     * @param item2 Second item
     * @return Text similarity score between 0 and 1
     */
    public static float calculateTextSimilarity(ReportedItem item1, ReportedItem item2) {
        Log.d(TAG, "🔤 Calculating text similarity between items: " + item1.getId() + " and " + item2.getId());
        long startTime = System.currentTimeMillis();
        
        if (item1 == null || item2 == null) {
            Log.e(TAG, "❌ Cannot calculate text similarity - null items");
            return 0;
        }
        
        float nameScore = calculateStringSimilarity(item1.getName(), item2.getName());
        float descriptionScore = calculateStringSimilarity(item1.getDescription(), item2.getDescription());
        float locationScore = calculateStringSimilarity(item1.getLocation(), item2.getLocation());
        
        // Log individual scores for debugging
        Log.d(TAG, "📊 Text similarity scores - Name: " + nameScore + 
              ", Description: " + descriptionScore + ", Location: " + locationScore);
        
        // Give more weight to name and location than description
        float finalScore = (nameScore * 0.4f) + (descriptionScore * 0.2f) + (locationScore * 0.4f);
        
        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "✅ Text similarity calculation completed in " + duration + 
              "ms, final score: " + finalScore);
        
        return finalScore;
    }
    
    /**
     * Calculate similarity between two strings using advanced fuzzy matching techniques
     * 
     * @param str1 First string
     * @param str2 Second string
     * @return Similarity score between 0 and 1
     */
    private static float calculateStringSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0;
        }
        
        // Trim and convert to lowercase for case-insensitive matching
        String s1 = str1.trim().toLowerCase();
        String s2 = str2.trim().toLowerCase();
        
        // Skip empty strings
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0;
        }
        
        // Exact match
        if (s1.equals(s2)) {
            return 1.0f;
        }
        
        // Check if one contains the other
        if (s1.contains(s2)) {
            return 0.9f;
        } else if (s2.contains(s1)) {
            return 0.9f;
        }
        
        // Check for word matches
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        
        // Count exact word matches
        int exactMatchCount = 0;
        // Count partial word matches
        int partialMatchCount = 0;
        
        for (String word1 : words1) {
            if (word1.length() <= 2) continue; // Skip very short words
            
            boolean foundExact = false;
            boolean foundPartial = false;
            
            for (String word2 : words2) {
                if (word2.length() <= 2) continue; // Skip very short words
                
                if (word1.equals(word2)) {
                    exactMatchCount++;
                    foundExact = true;
                    break;
                } else if (word1.contains(word2) || word2.contains(word1)) {
                    if (!foundExact) { // Only count partial if we don't have an exact match
                        partialMatchCount++;
                        foundPartial = true;
                    }
                } else if (levenshteinDistance(word1, word2) <= 2 && word1.length() > 3 && word2.length() > 3) {
                    // Small edit distance suggests a possible typo or slight variation
                    if (!foundExact && !foundPartial) {
                        partialMatchCount++;
                    }
                }
            }
        }
        
        // Calculate score based on word matches
        int maxWords = Math.max(words1.length, words2.length);
        if (maxWords == 0) {
            return 0;
        }
        
        // Weight exact matches more than partial matches
        float score = ((exactMatchCount * 1.0f) + (partialMatchCount * 0.5f)) / maxWords;
        
        // Cap the score at 1.0
        return Math.min(score, 1.0f);
    }
    
    /**
     * Calculate the Levenshtein distance between two strings
     * (minimum number of single-character edits to change one string into the other)
     */
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
    
    /**
     * Find matches for an item based on both text and image similarity
     * 
     * @param context Context for image processing
     * @param newItem The item to find matches for
     * @param oppositeItems List of items of the opposite type to check against
     * @param imageEmbeddings Map of item ID to image embeddings
     * @param newItemEmbedding Embedding for the new item
     * @return List of matches with combined similarity scores
     */
    public static List<MatchResult> findMatches(
            Context context, 
            ReportedItem newItem,
            List<ReportedItem> oppositeItems,
            Map<String, float[]> imageEmbeddings,
            float[] newItemEmbedding) {
        
        Log.d(TAG, "🔍 Finding matches for item: " + newItem.getId() + " among " + 
              oppositeItems.size() + " potential matches");
        long startTime = System.currentTimeMillis();
        
        List<MatchResult> matches = new ArrayList<>();
        int processedCount = 0;
        
        for (ReportedItem item : oppositeItems) {
            long itemStartTime = System.currentTimeMillis();
            processedCount++;
            
            // Calculate text similarity
            Log.d(TAG, "📝 Comparing item " + newItem.getId() + " with " + item.getId());
            float textSimilarity = calculateTextSimilarity(newItem, item);
            
            // Calculate image similarity if embeddings are available
            float imageSimilarity = 0;
            if (newItemEmbedding != null && imageEmbeddings.containsKey(item.getId())) {
                float[] itemEmbedding = imageEmbeddings.get(item.getId());
                if (itemEmbedding != null) {
                    imageSimilarity = ImageSimilaritySearch.calculateSimilarity(
                            newItemEmbedding, itemEmbedding);
                }
            }
            
            // Calculate combined score
            float combinedScore;
            if (newItemEmbedding != null && imageEmbeddings.containsKey(item.getId())) {
                // Both text and image available
                combinedScore = (textSimilarity * TEXT_MATCH_WEIGHT) + 
                                (imageSimilarity * IMAGE_MATCH_WEIGHT);
                
                Log.d(TAG, "📊 Combined score for " + item.getId() + ": " + combinedScore + 
                      " (Text: " + textSimilarity + ", Image: " + imageSimilarity + ")");
            } else {
                // Only text available
                combinedScore = textSimilarity;
                Log.d(TAG, "📝 Text-only score for " + item.getId() + ": " + combinedScore);
            }
            
            // Add to matches if score is above threshold
            if (combinedScore >= 0.3f) {
                matches.add(new MatchResult(item.getId(), combinedScore));
                Log.d(TAG, "✅ Found match: " + item.getId() + " with score: " + combinedScore);
            }
            
            long itemTime = System.currentTimeMillis() - itemStartTime;
            if (itemTime > 100) { // Log slow comparisons
                Log.w(TAG, "⚠️ Item comparison took too long: " + itemTime + "ms for " + item.getId());
            }
            
            // Log progress every 10 items or for the last item
            if (processedCount % 10 == 0 || processedCount == oppositeItems.size()) {
                Log.d(TAG, "🔄 Processed " + processedCount + "/" + oppositeItems.size() + " items");
            }
        }
        
        // Sort by similarity score
        Collections.sort(matches, (a, b) -> 
                Float.compare(b.getSimilarityScore(), a.getSimilarityScore()));
        
        long totalTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "🏁 Match finding completed in " + totalTime + "ms, found " + 
              matches.size() + " matches above threshold");
        
        return matches;
    }
    
    /**
     * Class representing a match result with its similarity score
     */
    public static class MatchResult {
        private String itemId;
        private float similarityScore;
        
        public MatchResult(String itemId, float similarityScore) {
            this.itemId = itemId;
            this.similarityScore = similarityScore;
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public float getSimilarityScore() {
            return similarityScore;
        }
    }
    
    /**
     * Listener interface for match results
     */
    public interface MatchListener {
        void onMatchesFound(List<ReportedItem> matchedItems, Map<String, Float> similarityScores);
        void onNoMatchesFound();
        void onError(String errorMessage);
    }
}
