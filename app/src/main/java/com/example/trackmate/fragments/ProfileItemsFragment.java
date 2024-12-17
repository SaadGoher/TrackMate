package com.example.trackmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackmate.R;
import com.example.trackmate.activities.ItemDetailActivity;
import com.example.trackmate.adapters.ReportedItemAdapter;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.SharedPrefsUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ProfileItemsFragment extends Fragment {
    private static final String ARG_TYPE = "type";
    private String type;
    private RecyclerView recyclerView;
    private ReportedItemAdapter adapter;
    private List<ReportedItem> itemsList;

    public static ProfileItemsFragment newInstance(String type) {
        ProfileItemsFragment fragment = new ProfileItemsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getString(ARG_TYPE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_items, container, false);
        
        recyclerView = view.findViewById(R.id.items_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columns in grid
        
        itemsList = new ArrayList<>();
        adapter = new ReportedItemAdapter(getContext(), itemsList, item -> {
            Intent intent = new Intent(getContext(), ItemDetailActivity.class);
            intent.putExtra("item_id", item.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        
        loadUserItems();
        
        return view;
    }

    private void loadUserItems() {
        String userId = SharedPrefsUtil.getUserId(requireContext());
        Query query = FirebaseService.getDatabase()
                .child("reported_items")
                .orderByChild("userId")
                .equalTo(userId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemsList.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    ReportedItem item = itemSnapshot.getValue(ReportedItem.class);
                    if (item != null && type.equals(item.getType().name().toLowerCase())) {
                        item.setId(itemSnapshot.getKey()); // Save the Firebase key
                        itemsList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}
