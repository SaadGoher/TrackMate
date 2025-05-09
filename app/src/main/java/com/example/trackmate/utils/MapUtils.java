package com.example.trackmate.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.trackmate.R;
import com.example.trackmate.models.ReportedItem;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class MapUtils {

    private static final String TAG = "MapUtils";

    /**
     * Converts a vector drawable to a BitmapDescriptor for use as a map marker.
     * 
     * @param context The context to get resources
     * @param vectorResId The resource ID of the vector drawable
     * @return A BitmapDescriptor created from the vector drawable, or null if there was an error
     */
    public static BitmapDescriptor vectorToBitmap(Context context, int vectorResId) {
        try {
            Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
            if (vectorDrawable == null) {
                Log.e(TAG, "Drawable not found for resource ID: " + vectorResId);
                return null;
            }
            
            int width = vectorDrawable.getIntrinsicWidth();
            int height = vectorDrawable.getIntrinsicHeight();
            
            // Ensure minimum size for visibility
            width = Math.max(width, 48);
            height = Math.max(height, 48);
            
            vectorDrawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.draw(canvas);
            
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error converting vector to bitmap", e);
            return null;
        }
    }
    
    /**
     * Creates a custom marker with item image and name
     * 
     * @param context The context
     * @param item The item to create a marker for
     * @param callback Callback to receive the created BitmapDescriptor
     */
    public static void createCustomMarker(Context context, ReportedItem item, CustomMarkerCallback callback) {
        try {
            View markerView = LayoutInflater.from(context).inflate(R.layout.custom_map_marker, null);
            
            ImageView markerImage = markerView.findViewById(R.id.marker_item_image);
            TextView markerText = markerView.findViewById(R.id.marker_item_name);
            LinearLayout statusIndicator = markerView.findViewById(R.id.marker_status_indicator);
            
            // Set item name
            markerText.setText(item.getName());
            
            // Set status color
            int statusColor = item.getType() == ReportedItem.Type.LOST ? 
                    ContextCompat.getColor(context, R.color.lost_item_color) : 
                    ContextCompat.getColor(context, R.color.found_item_color);
            statusIndicator.setBackgroundColor(statusColor);
            
            // If there's no image URL or it's empty, use default and create marker immediately
            if (item.getImageUrl() == null || item.getImageUrl().isEmpty()) {
                markerImage.setImageResource(R.drawable.item);
                callback.onMarkerCreated(createBitmapDescriptorFromView(markerView));
                return;
            }
            
            // Load the image using Glide
            Glide.with(context)
                .asBitmap()
                .load(item.getImageUrl())
                .placeholder(R.drawable.item)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        markerImage.setImageBitmap(resource);
                        callback.onMarkerCreated(createBitmapDescriptorFromView(markerView));
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        // Use placeholder if load is cleared
                        markerImage.setImageResource(R.drawable.item);
                        callback.onMarkerCreated(createBitmapDescriptorFromView(markerView));
                    }
                    
                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        // Use default image if load fails
                        markerImage.setImageResource(R.drawable.item);
                        callback.onMarkerCreated(createBitmapDescriptorFromView(markerView));
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error creating custom marker", e);
            callback.onMarkerCreated(null);
        }
    }
    
    /**
     * Converts a view to a BitmapDescriptor for use as a map marker
     * 
     * @param view The view to convert
     * @return BitmapDescriptor that can be used for a map marker
     */
    private static BitmapDescriptor createBitmapDescriptorFromView(View view) {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        
        Bitmap bitmap = Bitmap.createBitmap(
                view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    
    /**
     * Callback interface for async marker creation
     */
    public interface CustomMarkerCallback {
        void onMarkerCreated(BitmapDescriptor markerIcon);
    }
}
