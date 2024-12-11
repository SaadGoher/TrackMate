package com.example.trackmate.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import com.example.trackmate.R;
import com.example.trackmate.adapters.ReportedItemAdapter;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.services.FirebaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ReportedItemAdapter adapter;
    private List<ReportedItem> reportedItemList;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        reportedItemList = new ArrayList<>();
        adapter = new ReportedItemAdapter(getContext(), reportedItemList);
        recyclerView.setAdapter(adapter);

        loadReportedItems();

        return view;
    }

    private void loadReportedItems() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseService.getDatabase().child("reported_items").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                reportedItemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ReportedItem item = snapshot.getValue(ReportedItem.class);
                    reportedItemList.add(item);
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}

