package com.example.trackmate;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import com.example.trackmate.fragments.FoundFragment;
import com.example.trackmate.fragments.HomeFragment;
import com.example.trackmate.fragments.InfoFragment;
import com.example.trackmate.fragments.ProfileFragment;
import com.example.trackmate.fragments.ReportFragment;
import com.example.trackmate.utils.SharedPrefsUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.trackmate.R;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME = "HOME";
    private static final String TAG_FOUND = "FOUND";
    private static final String TAG_REPORT = "REPORT";
    private static final String TAG_INFO = "INFO";
    private static final String TAG_PROFILE = "PROFILE";

    private DrawerLayout drawerLayout;

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
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_home_24);
        toolbar.setTitleTextColor(getResources().getColor(R.color.black));
        toolbar.setTitle("TrackMate");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            } else if (itemId == R.id.nav_info) {
                selectedFragment = new InfoFragment();
                tag = TAG_INFO;
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
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_settings) {
                // Handle settings action
                return true;
            }
            return false;
        });
    }
}