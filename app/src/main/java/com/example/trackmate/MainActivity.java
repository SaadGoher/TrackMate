package com.example.trackmate;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.Menu;
import android.graphics.drawable.Drawable;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.trackmate.activities.ScanQrActivity;
import com.example.trackmate.activities.ShareProfileActivity;
import com.example.trackmate.fragments.AboutFragment;
import com.example.trackmate.fragments.ContactFragment;
import com.example.trackmate.fragments.FoundFragment;
import com.example.trackmate.fragments.HelpCenterFragment;
import com.example.trackmate.fragments.HomeFragment;
import com.example.trackmate.fragments.MapFragment;
import com.example.trackmate.fragments.NotificationFragment;
import com.example.trackmate.fragments.PolicyFragment;
import com.example.trackmate.fragments.ProfileFragment;
import com.example.trackmate.fragments.ReportFragment;
import com.example.trackmate.fragments.SettingsFragment;
import com.example.trackmate.fragments.TermsFragment;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.SharedPrefsUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME = "HOME";
    private static final String TAG_FOUND = "FOUND";
    private static final String TAG_REPORT = "REPORT";
    private static final String TAG_MAP = "MAP";
    private static final String TAG_PROFILE = "PROFILE";
    private static final String TAG_NOTIFICATIONS = "NOTIFICATIONS";

    private DrawerLayout drawerLayout;
    private boolean isNavigatingBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TrackMate);
        super.onCreate(savedInstanceState);

        if (!SharedPrefsUtil.isLoggedIn(this)) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        setupToolbar();
        setupBottomNavigation();
        setupDrawerMenu();

        // Set default fragment to HomeFragment
        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new HomeFragment(), TAG_HOME);
            transaction.commit();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        
        // Create navigation icon with white color
        Drawable navIcon = getResources().getDrawable(R.drawable.baseline_menu_24);
        navIcon.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        
        // Apply directly to toolbar before setting it as support action bar
        toolbar.setNavigationIcon(navIcon);
        toolbar.setNavigationOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });
        
        // Set colors and title
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setTitle("TrackMate");
        toolbar.setBackgroundColor(getResources().getColor(R.color.primary_light));
        
        // Now set as support action bar
        setSupportActionBar(toolbar);
        
        // Set overflow icon color to white
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
            toolbar.setOverflowIcon(overflowIcon);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        
        // Set QR code icon color to white
        for (int i = 0; i < menu.size(); i++) {
            Drawable drawable = menu.getItem(i).getIcon();
            if (drawable != null) {
                drawable.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
            }
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // We handle the menu icon directly through navigationOnClickListener now
        if (item.getItemId() == android.R.id.home && isNavigatingBack) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_share_profile) {
            Intent intent = new Intent(MainActivity.this, ShareProfileActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_scan_qr) {
            Intent intent = new Intent(MainActivity.this, ScanQrActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showBackButton(boolean show) {
        isNavigatingBack = show;
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        
        if (show) {
            // Show back arrow instead of menu
            Drawable backIcon = getResources().getDrawable(R.drawable.baseline_arrow_back_24);
            backIcon.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
            toolbar.setNavigationIcon(backIcon);
            
            // Handle back navigation
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        } else {
            // Show menu icon
            Drawable menuIcon = getResources().getDrawable(R.drawable.baseline_menu_24);
            menuIcon.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
            toolbar.setNavigationIcon(menuIcon);
            
            // Handle drawer opening
            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
    }

    public void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String tag = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                tag = TAG_HOME;
            } else if (itemId == R.id.nav_found) {
                selectedFragment = new FoundFragment();
                tag = TAG_FOUND;
            } else if (itemId == R.id.nav_report) {
                selectedFragment = new ReportFragment();
                tag = TAG_REPORT;
            } else if (itemId == R.id.nav_map) {
                selectedFragment = new MapFragment();
                tag = TAG_MAP;
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
                tag = TAG_PROFILE;
            }
            if (selectedFragment != null && tag != null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, selectedFragment, tag);
                transaction.commit();
            }
            return true;
        });
    }

    private void setupDrawerMenu() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        
        // Setup the navigation header with user data
        setupNavigationHeader(navigationView);
        
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;
            String tag = null;
            if (itemId == R.id.nav_notifications) {
                selectedFragment = new NotificationFragment();
                tag = TAG_NOTIFICATIONS;
            } else if (itemId == R.id.nav_help_center) {
                selectedFragment = new HelpCenterFragment();
                tag = "HELP_CENTER";
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
                tag = "SETTINGS";
            } else if (itemId == R.id.nav_policy) {
                selectedFragment = new PolicyFragment();
                tag = "POLICY";
            } else if (itemId == R.id.nav_terms) {
                selectedFragment = new TermsFragment();
                tag = "TERMS";
            } else if (itemId == R.id.nav_about) {
                selectedFragment = new AboutFragment();
                tag = "ABOUT";
            } else if (itemId == R.id.nav_contact) {
                selectedFragment = new ContactFragment();
                tag = "CONTACT";
            } else if (itemId == R.id.nav_sign_out) {
                // Sign out from Firebase
                FirebaseService.getAuth().signOut();
                
                // Clear local user data
                SharedPrefsUtil.setLoggedIn(this, false);
                SharedPrefsUtil.setUserId(this, null);
                
                // Navigate to sign in screen
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            if (selectedFragment != null && tag != null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, selectedFragment, tag);
                transaction.addToBackStack(null);
                transaction.commit();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    /**
     * Set up the navigation header with the current user's profile information
     * @param navigationView The NavigationView to update
     */
    private void setupNavigationHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        
        // Get header views
        ShapeableImageView profileImage = headerView.findViewById(R.id.nav_header_profile_image);
        TextView userName = headerView.findViewById(R.id.nav_header_username);
        TextView userEmail = headerView.findViewById(R.id.nav_header_email);
        
        // Get the current user
        if (FirebaseService.getCurrentUser() != null) {
            String userId = FirebaseService.getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseService.getDatabase().child("users").child(userId);
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // Set user name - use display name if available, otherwise full name
                        String displayName = user.getDisplayName();
                        if (displayName == null || displayName.isEmpty()) {
                            displayName = user.getFullName();
                        }
                        userName.setText(displayName);
                        
                        // Set user email
                        userEmail.setText(user.getEmail());
                        
                        // Load profile image if available
                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            Glide.with(MainActivity.this)
                                    .load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .into(profileImage);
                        }
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error - use default values
                    Log.e("MainActivity", "Failed to load user profile for drawer header", error.toException());
                }
            });
        }
    }
}