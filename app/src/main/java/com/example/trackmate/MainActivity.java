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

import com.example.trackmate.activities.ShareProfileActivity;
import com.example.trackmate.fragments.AboutFragment;
import com.example.trackmate.fragments.ContactFragment;
import com.example.trackmate.fragments.FoundFragment;
import com.example.trackmate.fragments.HomeFragment;
import com.example.trackmate.fragments.InfoFragment;
import com.example.trackmate.fragments.PolicyFragment;
import com.example.trackmate.fragments.ProfileFragment;
import com.example.trackmate.fragments.ReportFragment;
import com.example.trackmate.fragments.SettingsFragment;
import com.example.trackmate.fragments.TermsFragment;
import com.example.trackmate.services.FirebaseService;
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
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Create and set white navigation icon
        Drawable navIcon = getResources().getDrawable(R.drawable.baseline_menu_24);
        navIcon.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
        getSupportActionBar().setHomeAsUpIndicator(navIcon);
        
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setTitle("TrackMate");
        toolbar.setBackgroundColor(getResources().getColor(R.color.primary_light));

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
                drawable.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (isNavigatingBack) {
                onBackPressed();
                return true;
            }
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        } else if (item.getItemId() == R.id.action_qr_code) {
            Intent intent = new Intent(MainActivity.this, ShareProfileActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showBackButton(boolean show) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(show);
        isNavigatingBack = show;
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
            Fragment selectedFragment = null;
            String tag = null;
            if (itemId == R.id.nav_settings) {
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
                FirebaseService.getAuth().signOut();
                SharedPrefsUtil.setLoggedIn(this, false);
                SharedPrefsUtil.setUserId(this, null);
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            if (selectedFragment != null && tag != null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, selectedFragment, tag);
                transaction.commit();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
}