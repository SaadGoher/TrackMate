package com.example.trackmate.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.trackmate.R;
import com.example.trackmate.adapters.TermsPagerAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TermsFragment extends Fragment {
    
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private MaterialButton acceptButton;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terms, container, false);
        
        // Initialize views
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);
        acceptButton = view.findViewById(R.id.accept_terms_button);
        
        // Set up ViewPager with adapter
        setupViewPager();
        
        // Set up accept button
        setupAcceptButton();
        
        return view;
    }
    
    private void setupViewPager() {
        // Since we're not creating separate fragments for Terms and Privacy,
        // we'll use a custom adapter to handle the tab content switching
        TermsPagerAdapter adapter = new TermsPagerAdapter(requireActivity());
        viewPager.setAdapter(adapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch(position) {
                    case 0:
                        tab.setText("Terms of Service");
                        break;
                    case 1:
                        tab.setText("Privacy Policy");
                        break;
                }
            }
        ).attach();
    }
    
    private void setupAcceptButton() {
        acceptButton.setOnClickListener(v -> {
            // Save acceptance in SharedPreferences
            SharedPreferences prefs = requireActivity().getSharedPreferences(
                    "TrackMatePrefs", 
                    0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("terms_accepted", true);
            editor.putLong("terms_accepted_date", System.currentTimeMillis());
            editor.apply();
            
            // Show confirmation
            Toast.makeText(requireContext(), 
                    "You have accepted the Terms and Privacy Policy", 
                    Toast.LENGTH_SHORT).show();
        });
    }
}