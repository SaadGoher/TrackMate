package com.example.trackmate.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.trackmate.EditProfileActivity;
import com.example.trackmate.R;
import com.example.trackmate.adapters.ProfileViewPagerAdapter;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.models.User;
import com.example.trackmate.services.FirebaseService;
import com.example.trackmate.utils.SharedPrefsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {
    private ShapeableImageView profileImage;
    private TextView userName, userEmail;
    private TextView lostItemsCount, foundItemsCount;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImage = view.findViewById(R.id.profile_image);
        initializeViews(view);
        setupViewPager();
        loadUserProfile();
        setupEditButton(view);

        return view;
    }

    private void initializeViews(View view) {
        userName = view.findViewById(R.id.user_name);
        userEmail = view.findViewById(R.id.user_email);
        lostItemsCount = view.findViewById(R.id.lost_items_count);
        foundItemsCount = view.findViewById(R.id.found_items_count);
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);
        userId = SharedPrefsUtil.getUserId(requireContext());
    }

    private void setupViewPager() {
        ProfileViewPagerAdapter adapter = new ProfileViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Lost Items" : "Found Items");
        }).attach();
    }

    private void loadUserProfile() {
        if (FirebaseService.getCurrentUser() == null) {
            return;
        }

        String userId = FirebaseService.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseService.getDatabase().child("users").child(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                User user = task.getResult().getValue(User.class);
                if (user != null) {
                    userName.setText(user.getFullName());
                    userEmail.setText(user.getEmail());

                    // Load profile image using Glide with proper error handling
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(requireContext())
                            .load(user.getProfileImageUrl())
                            .placeholder(R.drawable.baseline_person_24)
                            .error(R.drawable.baseline_person_24)
                            .centerCrop()
                            .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.baseline_person_24);
                    }
                    // Load counts
                    loadItemCounts();
                }
            }
        });
    }

    private void loadItemCounts() {
        FirebaseService.getDatabase()
            .child("reported_items")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int lost = 0, found = 0;
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        ReportedItem item = itemSnapshot.getValue(ReportedItem.class);
                        if (item != null) {
                            if (item.getType() == ReportedItem.Type.LOST) {
                                lost++;
                            } else if (item.getType() == ReportedItem.Type.FOUND) {
                                found++;
                            }
                        }
                    }
                    lostItemsCount.setText(String.valueOf(lost));
                    foundItemsCount.setText(String.valueOf(found));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
    }

    private void setupEditButton(View view) {
        MaterialButton editButton = view.findViewById(R.id.edit_profile_button);
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload profile when returning to fragment (e.g., after editing)
        loadUserProfile();
    }
}

