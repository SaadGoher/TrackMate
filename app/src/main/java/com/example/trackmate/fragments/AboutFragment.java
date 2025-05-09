package com.example.trackmate.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trackmate.R;

public class AboutFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        
        // Get app version from BuildConfig or PackageInfo
        TextView versionTextView = view.findViewById(R.id.app_version);
        if (versionTextView != null) {
            String versionName = "Version 1.0.0"; // Ideally, get this dynamically
            versionTextView.setText(versionName);
        }
        
        // App logo click event (for demo purposes)
        ImageView appLogo = view.findViewById(R.id.app_logo);
        if (appLogo != null) {
            appLogo.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "TrackMate - Your fitness companion", Toast.LENGTH_SHORT).show();
            });
        }
        
        return view;
    }
}