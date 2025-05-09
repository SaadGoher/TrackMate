package com.example.trackmate.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.trackmate.R;
import com.example.trackmate.models.ReportedItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View mWindow;
    private final Context context;

    public CustomInfoWindowAdapter(Context context) {
        this.context = context;
        mWindow = LayoutInflater.from(context).inflate(R.layout.custom_map_marker_info, null);
    }

    private void renderWindowText(Marker marker, View view) {
        String title = marker.getTitle();
        TextView titleTextView = view.findViewById(R.id.title);
        
        if (title != null && !title.equals("")) {
            titleTextView.setText(title);
        } else {
            titleTextView.setText("Unknown Item");
        }

        String snippet = marker.getSnippet();
        TextView snippetTextView = view.findViewById(R.id.snippet);
        
        if (snippet != null && !snippet.equals("")) {
            // Colorize status text based on item type
            if (snippet.equals("Lost")) {
                snippetTextView.setTextColor(context.getResources().getColor(R.color.lost_item_color));
                snippetTextView.setText("Status: LOST");
            } else if (snippet.equals("Found")) {
                snippetTextView.setTextColor(context.getResources().getColor(R.color.found_item_color));
                snippetTextView.setText("Status: FOUND");
            } else {
                snippetTextView.setText(snippet);
            }
        } else {
            snippetTextView.setVisibility(View.GONE);
        }
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        renderWindowText(marker, mWindow);
        return mWindow;
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        return null;
    }
}
