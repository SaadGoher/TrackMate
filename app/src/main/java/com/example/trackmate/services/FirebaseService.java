package com.example.trackmate.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.example.trackmate.models.ImageEmbedding;
import com.example.trackmate.models.User;
import com.example.trackmate.models.ReportedItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static FirebaseAuth auth;
    private static DatabaseReference database;
    private static StorageReference storage;

    public static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
            Log.d(TAG, "🔐 Firebase Auth initialized");
        }
        return auth;
    }

    public static DatabaseReference getDatabase() {
        if (database == null) {
            database = FirebaseDatabase.getInstance().getReference();
            Log.d(TAG, "📊 Firebase Database initialized");
        }
        return database;
    }

    public static StorageReference getStorage() {
        if (storage == null) {
            storage = FirebaseStorage.getInstance().getReference();
        }
        return storage;
    }

    public static void signIn(String email, String password, OnCompleteListener<AuthResult> listener) {
        getAuth().signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public static FirebaseUser getCurrentUser() {
        return getAuth().getCurrentUser();
    }

    public static void createUser(String email, String password, OnCompleteListener<AuthResult> listener) {
        getAuth().createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public static void saveUserDetails(String userId, User userDetails) {
        DatabaseReference userRef = getDatabase().child("users").child(userId);
        userRef.setValue(userDetails);
    }

    public static void uploadImage(Uri imageUri, OnCompleteListener<Uri> listener) {
        StorageReference imageRef = getStorage().child("images/" + UUID.randomUUID().toString());
        imageRef.putFile(imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return imageRef.getDownloadUrl();
        }).addOnCompleteListener(listener);
    }

    public static void reportItem(String userId, ReportedItem item, OnCompleteListener<Void> listener) {
        Log.d(TAG, "📝 Reporting new item: " + item.getName() + " | Type: " + item.getType() + " | User: " + userId);
        long startTime = System.currentTimeMillis();
        
        DatabaseReference itemRef = getDatabase().child("reported_items").push();
        item.setUserId(userId);
        item.setId(itemRef.getKey());
        
        Log.d(TAG, "📌 Generated item ID: " + item.getId());
        
        itemRef.setValue(item).addOnCompleteListener(task -> {
            long duration = System.currentTimeMillis() - startTime;
            if (task.isSuccessful()) {
                Log.d(TAG, "✅ Successfully reported item: " + item.getId() + " in " + duration + "ms");
            } else {
                Log.e(TAG, "❌ Failed to report item: " + item.getId() + " after " + duration + "ms", task.getException());
            }
            listener.onComplete(task);
        });
    }

    public static Query getItemsByStatus(String userId, String type, String status) {
        return getDatabase()
                .child("reported_items")
                .orderByChild("userId")
                .equalTo(userId);
    }

    public static void updateItemStatus(String itemId, String status, OnCompleteListener<Void> listener) {
        getDatabase()
                .child("reported_items")
                .child(itemId)
                .child("status")
                .setValue(status)
                .addOnCompleteListener(listener);
    }
    
    /**
     * Save image embedding to Firebase
     * 
     * @param embedding The image embedding to save
     * @param listener Completion listener
     */
    public static void saveImageEmbedding(ImageEmbedding embedding, OnCompleteListener<Void> listener) {
        try {
            Log.d(TAG, "💾 Saving image embedding for item: " + embedding.getItemId() + " | Size: " + 
                  (embedding.getEmbedding() != null ? embedding.getEmbedding().length : 0) + " features");
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> embeddingMap = embedding.toMap();
            getDatabase()
                    .child("image_embeddings")
                    .child(embedding.getItemId())
                    .setValue(embeddingMap)
                    .addOnCompleteListener(task -> {
                        long duration = System.currentTimeMillis() - startTime;
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅ Successfully saved embedding for item: " + embedding.getItemId() + " in " + duration + "ms");
                        } else {
                            Log.e(TAG, "❌ Failed to save embedding for item: " + embedding.getItemId() + " after " + duration + "ms", task.getException());
                        }
                        listener.onComplete(task);
                    });
        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving embedding: " + e.getMessage(), e);
            if (listener != null) {
                listener.onComplete(Tasks.forException(e));
            }
        }
    }
    
    /**
     * Retrieve all image embeddings from Firebase
     * 
     * @param callback Callback to receive the list of embeddings
     */
    public static void getAllImageEmbeddings(EmbeddingsCallback callback) {
        try {
            getDatabase()
                    .child("image_embeddings")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            try {
                                List<ImageEmbedding> embeddings = new ArrayList<>();
                                for (DataSnapshot embeddingSnapshot : snapshot.getChildren()) {
                                    try {
                                        String itemId = embeddingSnapshot.child("itemId").getValue(String.class);
                                        Object embeddingObj = embeddingSnapshot.child("embedding").getValue();
                                        Long timestampObj = embeddingSnapshot.child("timestamp").getValue(Long.class);
                                        long timestamp = timestampObj != null ? timestampObj : System.currentTimeMillis();
                                        
                                        if (embeddingObj instanceof ArrayList) {
                                            ArrayList<?> embeddingList = (ArrayList<?>) embeddingObj;
                                            float[] embeddingArray = new float[embeddingList.size()];
                                            
                                            for (int i = 0; i < embeddingList.size(); i++) {
                                                Object value = embeddingList.get(i);
                                                if (value instanceof Double) {
                                                    embeddingArray[i] = ((Double) value).floatValue();
                                                } else if (value instanceof Long) {
                                                    embeddingArray[i] = ((Long) value).floatValue();
                                                } else if (value instanceof Float) {
                                                    embeddingArray[i] = (Float) value;
                                                } else if (value instanceof Integer) {
                                                    embeddingArray[i] = ((Integer) value).floatValue();
                                                }
                                            }
                                            
                                            ImageEmbedding embedding = new ImageEmbedding();
                                            embedding.setItemId(itemId);
                                            embedding.setEmbedding(embeddingArray);
                                            embedding.setTimestamp(timestamp);
                                            embeddings.add(embedding);
                                        }
                                    } catch (Exception e) {
                                        Log.e("FirebaseService", "Error processing embedding: " + e.getMessage(), e);
                                        // Continue to next embedding
                                    }
                                }
                                callback.onEmbeddingsLoaded(embeddings);
                            } catch (Exception e) {
                                Log.e("FirebaseService", "Error getting all embeddings: " + e.getMessage(), e);
                                callback.onError("Error retrieving embeddings: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("FirebaseService", "Firebase error: " + error.getMessage(), error.toException());
                            callback.onError(error.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e("FirebaseService", "Error retrieving all embeddings: " + e.getMessage(), e);
            callback.onError("Error retrieving embeddings: " + e.getMessage());
        }
    }
    
    /**
     * Retrieve image embeddings for a specific type (LOST or FOUND)
     * 
     * @param type The type of items (lost or found)
     * @param callback Callback to receive the list of embeddings
     */
    public static void getImageEmbeddingsByType(ReportedItem.Type type, EmbeddingsCallback callback) {
        try {
            // First get all items of the specified type
            getDatabase()
                    .child("reported_items")
                    .orderByChild("type")
                    .equalTo(type.toString())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            try {
                                List<String> itemIds = new ArrayList<>();
                                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                                    itemIds.add(itemSnapshot.getKey());
                                }
                                
                                if (itemIds.isEmpty()) {
                                    callback.onEmbeddingsLoaded(new ArrayList<>());
                                    return;
                                }
                                
                                // Then get embeddings for these items
                                getDatabase()
                                        .child("image_embeddings")
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                try {
                                                    List<ImageEmbedding> embeddings = new ArrayList<>();
                                                    for (DataSnapshot embeddingSnapshot : snapshot.getChildren()) {
                                                        try {
                                                            String itemId = embeddingSnapshot.child("itemId").getValue(String.class);
                                                            
                                                            if (itemIds.contains(itemId)) {
                                                                Object embeddingObj = embeddingSnapshot.child("embedding").getValue();
                                                                Long timestampObj = embeddingSnapshot.child("timestamp").getValue(Long.class);
                                                                long timestamp = timestampObj != null ? timestampObj : System.currentTimeMillis();
                                                                
                                                                if (embeddingObj instanceof ArrayList) {
                                                                    ArrayList<?> embeddingList = (ArrayList<?>) embeddingObj;
                                                                    float[] embeddingArray = new float[embeddingList.size()];
                                                                    
                                                                    for (int i = 0; i < embeddingList.size(); i++) {
                                                                        Object value = embeddingList.get(i);
                                                                        if (value instanceof Double) {
                                                                            embeddingArray[i] = ((Double) value).floatValue();
                                                                        } else if (value instanceof Long) {
                                                                            embeddingArray[i] = ((Long) value).floatValue();
                                                                        } else if (value instanceof Float) {
                                                                            embeddingArray[i] = (Float) value;
                                                                        } else if (value instanceof Integer) {
                                                                            embeddingArray[i] = ((Integer) value).floatValue();
                                                                        }
                                                                    }
                                                    
                                                                    ImageEmbedding embedding = new ImageEmbedding();
                                                                    embedding.setItemId(itemId);
                                                                    embedding.setEmbedding(embeddingArray);
                                                                    embedding.setTimestamp(timestamp);
                                                                    embeddings.add(embedding);
                                                                }
                                                            }
                                                        } catch (Exception e) {
                                                            Log.e("FirebaseService", "Error processing embedding: " + e.getMessage(), e);
                                                            // Continue to next embedding
                                                        }
                                                    }
                                                    callback.onEmbeddingsLoaded(embeddings);
                                                } catch (Exception e) {
                                                    Log.e("FirebaseService", "Error getting embeddings: " + e.getMessage(), e);
                                                    callback.onError("Error retrieving embeddings: " + e.getMessage());
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("FirebaseService", "Firebase error: " + error.getMessage(), error.toException());
                                                callback.onError(error.getMessage());
                                            }
                                        });
                            } catch (Exception e) {
                                Log.e("FirebaseService", "Error processing items: " + e.getMessage(), e);
                                callback.onError("Error processing items: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("FirebaseService", "Firebase error: " + error.getMessage(), error.toException());
                            callback.onError(error.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e("FirebaseService", "Error retrieving embeddings by type: " + e.getMessage(), e);
            callback.onError("Error retrieving embeddings: " + e.getMessage());
        }
    }
    
    /**
     * Callback interface for retrieving embeddings
     */
    public interface EmbeddingsCallback {
        void onEmbeddingsLoaded(List<ImageEmbedding> embeddings);
        void onError(String errorMessage);
    }
}
