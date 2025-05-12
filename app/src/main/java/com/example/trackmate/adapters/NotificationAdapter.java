package com.example.trackmate.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.example.trackmate.R;
import com.example.trackmate.models.Notification;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Notification notification);
    }

    private static final String TAG = "NotificationAdapter";
    private Context context;
    private List<Notification> notifications;
    private OnItemClickListener listener;
    
    public NotificationAdapter(Context context, List<Notification> notifications, OnItemClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        
        // Set notification title and message
        holder.titleView.setText(notification.getTitle());
        holder.messageView.setText(notification.getMessage());
        
        // Set timestamp
        holder.timeView.setText(formatTimestamp(notification.getTimestamp()));
        
        // Show icon based on notification type (if item_id exists, it's a match notification)
        if (notification.getItemId() != null) {
            holder.iconView.setImageResource(R.drawable.ic_notification_match);
        } else {
            holder.iconView.setImageResource(R.drawable.ic_notification_info);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView titleView;
        TextView messageView;
        TextView timeView;

        ViewHolder(View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.notification_icon);
            titleView = itemView.findViewById(R.id.notification_title);
            messageView = itemView.findViewById(R.id.notification_message);
            timeView = itemView.findViewById(R.id.notification_time);
        }
    }

    private String formatTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        // Less than 24 hours
        if (diff < 24 * 60 * 60 * 1000) {
            return android.text.format.DateUtils.getRelativeTimeSpanString(timestamp, now, android.text.format.DateUtils.MINUTE_IN_MILLIS,
                    android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        }
        
        // Less than 7 days
        if (diff < 7 * 24 * 60 * 60 * 1000) {
            return android.text.format.DateUtils.getRelativeTimeSpanString(timestamp, now, android.text.format.DateUtils.DAY_IN_MILLIS,
                    android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        }
        
        // More than 7 days
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(timestamp));
    }
}
