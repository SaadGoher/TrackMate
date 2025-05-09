package com.example.trackmate.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackmate.R;
import com.example.trackmate.activities.ItemDetailActivity;
import com.example.trackmate.activities.ScanQrActivity;
import com.example.trackmate.adapters.ReportedItemAdapter;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.SearchUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.textfield.TextInputEditText;
import android.view.inputmethod.EditorInfo;
import java.util.ArrayList;
import java.util.List;
import com.google.android.material.card.MaterialCardView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.trackmate.MainActivity;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ReportedItemAdapter adapter;
    private List<ReportedItem> reportedItemList;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private List<ReportedItem> allItems = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Reset toolbar to default state
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.showBackButton(false);
            activity.setToolbarTitle("TrackMate");
        }

        // Find the messages card and set click listener
        MaterialCardView messagesCard = view.findViewById(R.id.messages_card);
        messagesCard.setOnClickListener(v -> {
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new MessageFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });
        
        // Find QR code scan card and set click listener
        MaterialCardView qrScanCard = view.findViewById(R.id.qr_scan_card);
        qrScanCard.setOnClickListener(v -> {
            if (getActivity() != null) {
                if (checkCameraPermission()) {
                    launchQrScanner();
                }
            }
        });

        TextInputEditText searchInput = view.findViewById(R.id.search_input);
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchInput.getText().toString());
                return true;
            }
            return false;
        });

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columns in grid

        reportedItemList = new ArrayList<>();
        adapter = new ReportedItemAdapter(getContext(), reportedItemList, item -> {
            Intent intent = new Intent(getContext(), ItemDetailActivity.class);
            intent.putExtra("item_id", item.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadReportedItems();

        return view;
    }

    private boolean checkCameraPermission() {
        if (getActivity() != null && ContextCompat.checkSelfPermission(getActivity(), 
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestCameraPermission();
            return false;
        }
        return true;
    }
    
    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }
    
    private void launchQrScanner() {
        Intent intent = new Intent(getActivity(), ScanQrActivity.class);
        startActivity(intent);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                launchQrScanner();
            } else {
                // Permission denied
                Toast.makeText(getActivity(), "Camera permission is required to scan QR codes", 
                        Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            adapter.updateList(allItems);
            return;
        }
        List<ReportedItem> filteredList = SearchUtils.filterItems(allItems, query);
        adapter.updateList(filteredList);
    }

    private void loadReportedItems() {
        Query query = FirebaseService.getDatabase().child("reported_items");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allItems.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    ReportedItem item = itemSnapshot.getValue(ReportedItem.class);
                    if (item != null && item.getType() == ReportedItem.Type.LOST) {
                        item.setId(itemSnapshot.getKey());
                        allItems.add(item);
                    }
                }
                adapter.updateList(allItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}

