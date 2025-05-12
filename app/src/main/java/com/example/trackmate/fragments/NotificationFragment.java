package com.example.trackmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.trackmate.R;
import com.example.trackmate.activities.ItemDetailActivity;
import com.example.trackmate.activities.NotificationDetailActivity;
import com.example.trackmate.adapters.NotificationAdapter;
import com.example.trackmate.models.Notification;
import com.example.trackmate.services.FirebaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationFragment extends Fragment {

    private static final String TAG = "NotificationFragment";
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private TextView noNotifications;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        noNotifications = view.findViewById(R.id.no_notifications);
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Enable swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
        
        // Setup notifications list with swipe-to-delete
        notificationList = new ArrayList<>();
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Notification notification = notificationList.get(position);
                deleteNotification(notification, position);
            }
        };
        
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);

        adapter = new NotificationAdapter(getContext(), notificationList, notification -> {
            // Handle notification click based on type
            if (notification.getItemId() != null) {
                // Open related item details
                Intent intent = new Intent(getContext(), ItemDetailActivity.class);
                intent.putExtra("item_id", notification.getItemId());
                startActivity(intent);
            } else {
                // Open notification details
                Intent intent = new Intent(getContext(), NotificationDetailActivity.class);
                intent.putExtra("notification_id", notification.getId());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        loadNotifications();

        return view;
    }

    private void loadNotifications() {
        if (FirebaseService.getCurrentUser() == null) {
            noNotifications.setVisibility(View.VISIBLE);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        Query query = FirebaseService.getDatabase().child("notifications")
                .orderByChild("userId")
                .equalTo(FirebaseService.getCurrentUser().getUid());

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notificationList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Notification notification = snapshot.getValue(Notification.class);
                        if (notification != null) {
                            notification.setId(snapshot.getKey()); // Ensure ID is set
                            notificationList.add(notification);
                        }
                    }
                    
                    // Sort notifications by timestamp (newest first)
                    Collections.sort(notificationList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    
                    adapter.notifyDataSetChanged();
                    noNotifications.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    noNotifications.setVisibility(View.VISIBLE);
                }
                
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                noNotifications.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Delete a notification and handle UI update
     */
    private void deleteNotification(Notification notification, int position) {
        if (notification.getId() == null) return;

        FirebaseService.deleteNotification(notification.getId(), task -> {
            if (task.isSuccessful()) {
                notificationList.remove(position);
                adapter.notifyItemRemoved(position);
                
                if (notificationList.isEmpty()) {
                    noNotifications.setVisibility(View.VISIBLE);
                }
                
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Notification deleted", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Restore the item if deletion fails
                adapter.notifyItemChanged(position);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to delete notification", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
