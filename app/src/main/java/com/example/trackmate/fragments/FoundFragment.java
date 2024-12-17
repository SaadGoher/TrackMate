package com.example.trackmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackmate.R;
import com.example.trackmate.activities.ItemDetailActivity;
import com.example.trackmate.adapters.ReportedItemAdapter;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.services.FirebaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class FoundFragment extends Fragment {

    private RecyclerView recyclerView;
    private ReportedItemAdapter adapter;
    private List<ReportedItem> foundItemList;
    private EditText searchInput;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_found, container, false);

        searchInput = view.findViewById(R.id.search_input);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columns in grid

        foundItemList = new ArrayList<>();
        adapter = new ReportedItemAdapter(getContext(), foundItemList, item -> {
            Intent intent = new Intent(getContext(), ItemDetailActivity.class);
            intent.putExtra("item_id", item.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadFoundItems();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });

        return view;
    }

    private void loadFoundItems() {
        Query query = FirebaseService.getDatabase().child("reported_items");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                foundItemList.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    ReportedItem item = itemSnapshot.getValue(ReportedItem.class);
                    if (item != null && item.getType() == ReportedItem.Type.FOUND) { // Only add found items
                        item.setId(itemSnapshot.getKey()); // Save the Firebase key
                        foundItemList.add(item);
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

    private void filter(String text) {
        List<ReportedItem> filteredList = new ArrayList<>();
        for (ReportedItem item : foundItemList) {
            if (item.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
    }
}

