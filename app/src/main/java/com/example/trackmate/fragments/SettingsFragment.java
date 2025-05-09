package com.example.trackmate.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trackmate.R;
import com.google.android.material.button.MaterialButton;

public class SettingsFragment extends Fragment {
    
    // UI Elements
    private LinearLayout editProfileOption;
    private LinearLayout changePasswordOption;
    private LinearLayout themeOption;
    private SwitchCompat pushNotificationsSwitch;
    private SwitchCompat emailNotificationsSwitch;
    private SwitchCompat locationSwitch;
    private MaterialButton clearCacheButton;
    private TextView themeValue;
    
    // Preferences
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "TrackMatePrefs";
    private static final String KEY_PUSH_NOTIFICATIONS = "push_notifications";
    private static final String KEY_EMAIL_NOTIFICATIONS = "email_notifications";
    private static final String KEY_LOCATION_SERVICES = "location_services";
    private static final String KEY_THEME = "app_theme";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        initializeViews(view);
        loadPreferences();
        setupListeners();
        return view;
    }
    
    private void initializeViews(View view) {
        // Account Settings
        editProfileOption = view.findViewById(R.id.edit_profile_option);
        changePasswordOption = view.findViewById(R.id.change_password_option);
        
        // Notification Settings
        pushNotificationsSwitch = view.findViewById(R.id.push_notifications_switch);
        emailNotificationsSwitch = view.findViewById(R.id.email_notifications_switch);
        
        // App Settings
        themeOption = view.findViewById(R.id.theme_option);
        themeValue = view.findViewById(R.id.theme_value);
        locationSwitch = view.findViewById(R.id.location_switch);
        
        // Other
        clearCacheButton = view.findViewById(R.id.clear_cache_button);
    }
    
    private void loadPreferences() {
        preferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Load saved preferences with defaults
        pushNotificationsSwitch.setChecked(preferences.getBoolean(KEY_PUSH_NOTIFICATIONS, true));
        emailNotificationsSwitch.setChecked(preferences.getBoolean(KEY_EMAIL_NOTIFICATIONS, false));
        locationSwitch.setChecked(preferences.getBoolean(KEY_LOCATION_SERVICES, true));
        
        // Load theme setting
        String theme = preferences.getString(KEY_THEME, "Light");
        themeValue.setText(theme);
    }
    
    private void setupListeners() {
        // Account Settings
        editProfileOption.setOnClickListener(v -> {
            // Navigate to edit profile screen or show dialog
            Toast.makeText(requireContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to EditProfileActivity or show dialog
        });
        
        changePasswordOption.setOnClickListener(v -> {
            // Navigate to change password screen or show dialog
            Toast.makeText(requireContext(), "Change Password clicked", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to ChangePasswordActivity or show dialog
        });
        
        // Notification Settings
        pushNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_PUSH_NOTIFICATIONS, isChecked);
            editor.apply();
            
            String message = isChecked ? "Push notifications enabled" : "Push notifications disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
        
        emailNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_EMAIL_NOTIFICATIONS, isChecked);
            editor.apply();
            
            String message = isChecked ? "Email notifications enabled" : "Email notifications disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
        
        // App Settings
        themeOption.setOnClickListener(v -> {
            // Show theme selection dialog
            showThemeDialog();
        });
        
        locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_LOCATION_SERVICES, isChecked);
            editor.apply();
            
            String message = isChecked ? "Location services enabled" : "Location services disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
        
        // Clear Cache Button
        clearCacheButton.setOnClickListener(v -> {
            // Show confirmation dialog
            showClearCacheConfirmationDialog();
        });
    }
    
    private void showThemeDialog() {
        String[] themes = {"Light", "Dark", "System default"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Theme");
        
        // Get current theme index
        String currentTheme = preferences.getString(KEY_THEME, "Light");
        int selectedIndex = 0;
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].equals(currentTheme)) {
                selectedIndex = i;
                break;
            }
        }
        
        builder.setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
            // Save selected theme
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_THEME, themes[which]);
            editor.apply();
            
            // Update UI
            themeValue.setText(themes[which]);
            
            // Show confirmation
            Toast.makeText(requireContext(), themes[which] + " theme selected", Toast.LENGTH_SHORT).show();
            
            // TODO: Apply theme change
            
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    
    private void showClearCacheConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Clear Cache");
        builder.setMessage("Are you sure you want to clear the app cache? This will delete temporary files and may log you out of the app.");
        
        builder.setPositiveButton("Clear", (dialog, which) -> {
            // Perform cache clearing operation
            clearAppCache();
            Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    
    private void clearAppCache() {
        // TODO: Implement actual cache clearing logic
        // This would typically involve:
        // 1. Clearing shared preferences (except critical ones)
        // 2. Clearing any disk caches
        // 3. Clearing in-memory caches
        
        // For demo purposes, we'll just show a toast
        Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show();
    }
}
    
