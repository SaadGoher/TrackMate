package com.example.trackmate.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.trackmate.R;
import com.google.android.material.card.MaterialCardView;

/**
 * Adapter to handle the ViewPager2 for Terms of Service and Privacy Policy tabs
 */
public class TermsPagerAdapter extends RecyclerView.Adapter<TermsPagerAdapter.PageViewHolder> {
    
    private final FragmentActivity activity;
    
    // Content for each tab
    private final String[] termsContent = new String[]{
        // Terms of Service content
        "1. Introduction\n\n" +
        "Welcome to TrackMate. By accessing or using our mobile application, you agree to be bound by these Terms of Service. If you disagree with any part of the terms, you may not access the service.\n\n" +
        "2. User Accounts\n\n" +
        "When you create an account with us, you must provide information that is accurate, complete, and current at all times. Failure to do so constitutes a breach of the Terms, which may result in immediate termination of your account on our service.\n\n" +
        "You are responsible for safeguarding the password that you use to access the service and for any activities or actions under your password. You agree not to disclose your password to any third party.\n\n" +
        "3. Intellectual Property\n\n" +
        "The service and its original content, features, and functionality are and will remain the exclusive property of TrackMate and its licensors. The service is protected by copyright, trademark, and other laws of both the United States and foreign countries. Our trademarks and trade dress may not be used in connection with any product or service without the prior written consent of TrackMate.",
        
        // Privacy Policy content
        "Privacy Policy\n\n" +
        "Last updated: May 1, 2023\n\n" +
        "1. Information We Collect\n\n" +
        "We collect several different types of information for various purposes to provide and improve our Service to you:\n\n" +
        "- Personal Data: While using our Service, we may ask you to provide us with certain personally identifiable information that can be used to contact or identify you (\"Personal Data\"). This may include, but is not limited to: Email address, First name and last name, Phone number, Address, Location data.\n\n" +
        "- Usage Data: We may also collect information on how the Service is accessed and used (\"Usage Data\"). This Usage Data may include information such as your computer's Internet Protocol address, browser type, browser version, the pages of our Service that you visit, the time and date of your visit, the time spent on those pages, unique device identifiers and other diagnostic data.\n\n" +
        "2. Use of Data\n\n" +
        "TrackMate uses the collected data for various purposes:\n\n" +
        "- To provide and maintain our Service\n" +
        "- To notify you about changes to our Service\n" +
        "- To allow you to participate in interactive features of our Service when you choose to do so\n" +
        "- To provide customer support\n" +
        "- To gather analysis or valuable information so that we can improve our Service\n" +
        "- To monitor the usage of our Service\n" +
        "- To detect, prevent and address technical issues\n\n" +
        "3. Data Security\n\n" +
        "The security of your data is important to us but remember that no method of transmission over the Internet or method of electronic storage is 100% secure. While we strive to use commercially acceptable means to protect your Personal Data, we cannot guarantee its absolute security."
    };
    
    public TermsPagerAdapter(FragmentActivity activity) {
        this.activity = activity;
    }
    
    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_terms_page, parent, false);
        return new PageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.contentText.setText(termsContent[position]);
        
        // Change title based on position
        if (position == 0) {
            holder.titleText.setText("Terms of Service");
        } else {
            holder.titleText.setText("Privacy Policy");
        }
    }
    
    @Override
    public int getItemCount() {
        return termsContent.length;
    }
    
    static class PageViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView contentText;
        
        PageViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.page_title);
            contentText = itemView.findViewById(R.id.page_content);
        }
    }
}
