package com.example.trackmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackmate.R;
import com.example.trackmate.activities.NotificationDetailActivity;
import com.example.trackmate.adapters.NotificationAdapter;
import com.example.trackmate.models.Notification;
import com.example.trackmate.services.FirebaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private TextView noNotifications;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        noNotifications = view.findViewById(R.id.no_notifications);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(getContext(), notificationList, notification -> {
            Intent intent = new Intent(getContext(), NotificationDetailActivity.class);
            intent.putExtra("notification_id", notification.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadNotifications();

        return view;
    }

    private void loadNotifications() {
        if (FirebaseService.getCurrentUser() == null) {
            noNotifications.setVisibility(View.VISIBLE);
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
                            notificationList.add(notification);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    noNotifications.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    noNotifications.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                noNotifications.setVisibility(View.VISIBLE);
            }
        });
    }
}
