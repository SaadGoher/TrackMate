package com.example.trackmate.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.models.Message;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<Message> messageList;
    private String currentUserId;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.getSenderId().equals(currentUserId) ? R.layout.item_message_right : R.layout.item_message_left;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messageList.get(position);
        
        // Set message text
        if (message.getText() != null && !message.getText().isEmpty()) {
            holder.messageText.setVisibility(View.VISIBLE);
            holder.messageText.setText(message.getText());
        } else {
            holder.messageText.setVisibility(View.GONE);
        }
        
        // Format timestamp
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(message.getTimestamp());
        String timeString = DateFormat.format("HH:mm", calendar).toString();
        holder.messageTime.setText(timeString);
        
        // Handle image if present
        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            holder.messageImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                 .load(message.getImageUrl())
                 .centerCrop()
                 .placeholder(R.drawable.baseline_image_24)
                 .error(R.drawable.baseline_broken_image_24)
                 .into(holder.messageImage);
        } else {
            holder.messageImage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public ImageView messageImage;
        public TextView messageTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            messageImage = itemView.findViewById(R.id.message_image);
            messageTime = itemView.findViewById(R.id.message_time);
        }
    }
}

