package com.example.trackmate.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.trackmate.R;
import com.example.trackmate.activities.ItemDetailActivity;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.CustomInfoWindowAdapter;
import com.example.trackmate.utils.MapUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private Map<String, Marker> markers = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("MapFragment", "Map is ready");
        mMap = googleMap;
        
        // Set custom info window adapter
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(requireContext()));
        Log.d("MapFragment", "Set custom info window adapter");
        
        // Check for location permission and enable my location
        enableMyLocation();
        
        // Configure map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        
        // Apply a custom map style
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        Log.d("MapFragment", "Map UI settings configured");
        
        // Load reported items on the map
        loadReportedItems();
        
        // Set up marker click listener
        mMap.setOnInfoWindowClickListener(marker -> {
            String itemId = (String) marker.getTag();
            if (itemId != null) {
                Log.d("MapFragment", "Marker clicked: " + marker.getTitle() + ", Item ID: " + itemId);
                // Navigate to item details
                Intent intent = new Intent(getActivity(), ItemDetailActivity.class);
                intent.putExtra("item_id", itemId);
                startActivity(intent);
            } else {
                Log.w("MapFragment", "Marker clicked but no item ID found");
            }
        });
    }
    
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            
            // Get current location and move camera
            LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
            try {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    LatLng currentLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                } else {
                    // Default location if no last known location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 2));
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    private void loadReportedItems() {
        Log.d("MapFragment", "Starting to load reported items");
        // Important: Check if the database reference path is correct
        // The ItemDetailActivity is using "reported_items" (with underscore)
        DatabaseReference itemsRef = FirebaseService.getDatabase().child("reported_items");
        
        Log.d("MapFragment", "Database reference path: " + itemsRef.toString());
        
        itemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("MapFragment", "Data snapshot received: " + dataSnapshot.getChildrenCount() + " items");
                
                // Clear existing markers
                for (Marker marker : markers.values()) {
                    marker.remove();
                }
                markers.clear();
                
                // To track map bounds for all items
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                boolean hasValidItems = false;
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ReportedItem item = snapshot.getValue(ReportedItem.class);
                    
                    if (item != null) {
                        // Set the ID from the Firebase key
                        item.setId(snapshot.getKey());
                        
                        Log.d("MapFragment", "Item: " + item.getName() + ", ID: " + item.getId() + 
                              ", Type: " + item.getType() + ", Location: " + item.getLatitude() + "," + item.getLongitude());
                        
                        if (item.getLatitude() != 0 && item.getLongitude() != 0) {
                            LatLng itemLocation = new LatLng(item.getLatitude(), item.getLongitude());
                            boundsBuilder.include(itemLocation);
                            hasValidItems = true;
                            
                            // Create marker options with position and basic info
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(itemLocation)
                                    .title(item.getName())
                                    .snippet(item.getType() == ReportedItem.Type.LOST ? "Lost" : "Found")
                                    .anchor(0.5f, 1.0f); // Center horizontally, align bottom vertically
                            
                            // Use our custom marker creator to make a marker with image and name
                            final String itemId = item.getId(); // Need final for lambda
                            MapUtils.createCustomMarker(requireContext(), item, markerIcon -> {
                                if (markerIcon != null) {
                                    markerOptions.icon(markerIcon);
                                    // Must be added on main thread
                                    requireActivity().runOnUiThread(() -> {
                                        try {
                                            Marker marker = mMap.addMarker(markerOptions);
                                            if (marker != null) {
                                                marker.setTag(itemId);
                                                markers.put(itemId, marker);
                                                Log.d("MapFragment", "Added custom marker for item: " + item.getName());
                                            } else {
                                                Log.e("MapFragment", "Failed to add custom marker for item: " + item.getName());
                                            }
                                        } catch (Exception e) {
                                            Log.e("MapFragment", "Error adding marker to map", e);
                                        }
                                    });
                                } else {
                                    // Fallback to simple colored markers if custom marker creation fails
                                    float markerColor = item.getType() == ReportedItem.Type.LOST ? 
                                            BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN;
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(markerColor));
                                    
                                    requireActivity().runOnUiThread(() -> {
                                        try {
                                            Marker marker = mMap.addMarker(markerOptions);
                                            if (marker != null) {
                                                marker.setTag(itemId);
                                                markers.put(itemId, marker);
                                                Log.d("MapFragment", "Added fallback marker for item: " + item.getName());
                                            }
                                        } catch (Exception e) {
                                            Log.e("MapFragment", "Error adding fallback marker", e);
                                        }
                                    });
                                }
                            });
                        } else {
                            Log.w("MapFragment", "Item has invalid location coordinates: " + item.getName());
                        }
                    } else {
                        Log.e("MapFragment", "Failed to parse item from data snapshot");
                    }
                }
                
                Log.d("MapFragment", "Processed " + dataSnapshot.getChildrenCount() + " items, markers to be added: " + dataSnapshot.getChildrenCount());
                
                // We need to adjust the camera after a delay to make sure all markers are added
                // Since we're creating markers asynchronously
                if (hasValidItems) {
                    // Post with delay to allow markers to be added
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (getActivity() == null || !isAdded()) {
                            return; // Fragment not attached
                        }
                        
                        Log.d("MapFragment", "Adjusting camera after delay, markers count: " + markers.size());
                        
                        if (!markers.isEmpty()) {
                            try {
                                // Create bounds for all markers
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (Marker marker : markers.values()) {
                                    builder.include(marker.getPosition());
                                }
                                
                                // Add some padding to the bounds
                                int padding = 150; // pixels
                                LatLngBounds bounds = builder.build();
                                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                                Log.d("MapFragment", "Animated camera to show all markers");
                            } catch (Exception e) {
                                Log.e("MapFragment", "Error animating camera", e);
                                // Handle the case where bounds building fails - zoom out to world view
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 2));
                            }
                        } else {
                            Log.d("MapFragment", "No markers to show after delay");
                        }
                    }, 1000); // Wait 1 second to ensure markers are loaded
                } else {
                    Log.d("MapFragment", "No valid items to show on map");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MapFragment", "Firebase database error: " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(getContext(), "Failed to load items: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
