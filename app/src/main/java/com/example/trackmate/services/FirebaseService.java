package com.example.trackmate.services;

import android.content.Context;
import android.net.Uri;
import com.example.trackmate.models.User;
import com.example.trackmate.models.ReportedItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.database.Query;

import java.util.UUID;

public class FirebaseService {
    private static FirebaseAuth auth;
    private static DatabaseReference database;
    private static StorageReference storage;

    public static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }

    public static DatabaseReference getDatabase() {
        if (database == null) {
            database = FirebaseDatabase.getInstance().getReference();
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
        DatabaseReference itemRef = getDatabase().child("reported_items").push();
        item.setUserId(userId);
        item.setId(itemRef.getKey());
        itemRef.setValue(item).addOnCompleteListener(listener);
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
}
