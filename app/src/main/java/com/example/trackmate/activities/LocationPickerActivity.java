package com.example.trackmate.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.trackmate.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private Marker selectedLocationMarker;
    private LatLng selectedLatLng;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean initialLocationSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);
        
        setupToolbar();
        
        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        Button selectLocationButton = findViewById(R.id.btn_select_location);
        selectLocationButton.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLatLng.latitude);
                resultIntent.putExtra("longitude", selectedLatLng.longitude);
                
                // Get address from coordinates
                try {
                    String address = getAddressFromLocation(selectedLatLng.latitude, selectedLatLng.longitude);
                    resultIntent.putExtra("address", address);
                } catch (Exception e) {
                    resultIntent.putExtra("address", "Unknown location");
                }
                
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Pick Location");
            
            // Set the back arrow icon instead of hamburger menu
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24);
            
            // Make sure the toolbar text is properly colored
            toolbar.setTitleTextColor(getResources().getColor(R.color.white));
            
            // Ensure the back icon is also properly colored
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setColorFilter(
                    getResources().getColor(R.color.white), 
                    android.graphics.PorterDuff.Mode.SRC_IN
                );
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Enable UI controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        
        // Check for location permissions
        enableMyLocation();
        
        // Set click listener for map to update marker
        mMap.setOnMapClickListener(latLng -> {
            updateSelectedLocation(latLng);
        });
    }
    
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                getCurrentLocation();
            }
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && !initialLocationSet) {
                initialLocationSet = true;
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                updateSelectedLocation(currentLatLng);
            }
        });
    }
    
    private void updateSelectedLocation(LatLng latLng) {
        // Remove previous marker if exists
        if (selectedLocationMarker != null) {
            selectedLocationMarker.remove();
        }
        
        // Add marker at selected location
        selectedLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Selected Location"));
        
        // Update selected location coordinates
        selectedLatLng = latLng;
        
        // Show address in marker title if possible
        try {
            String address = getAddressFromLocation(latLng.latitude, latLng.longitude);
            if (selectedLocationMarker != null) {
                selectedLocationMarker.setTitle(address);
                selectedLocationMarker.showInfoWindow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getAddressFromLocation(double latitude, double longitude) throws IOException {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
        
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            StringBuilder sb = new StringBuilder();
            
            // Get address lines
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(address.getAddressLine(i));
            }
            
            return sb.toString();
        }
        
        return "Unknown location";
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission is required to select your current location", Toast.LENGTH_LONG).show();
            }
        }
    }
}
