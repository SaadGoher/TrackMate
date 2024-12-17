package com.example.trackmate.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.trackmate.fragments.ProfileItemsFragment;

public class ProfileViewPagerAdapter extends FragmentStateAdapter {

    public ProfileViewPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Create fragment instances with different types
        return ProfileItemsFragment.newInstance(position == 0 ? "lost" : "found");
    }

    @Override
    public int getItemCount() {
        return 2; // Two tabs: Lost and Found
    }
}
    
