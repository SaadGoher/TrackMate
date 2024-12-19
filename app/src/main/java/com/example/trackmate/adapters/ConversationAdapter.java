package com.example.trackmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.models.Message;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
    private List<Message> conversations;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(String userId);
    }

    public ConversationAdapter(List<Message> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message conversation = conversations.get(position);
        holder.userName.setText(conversation.getReceiverName());
        
        if (conversation.getText() != null) {
            holder.lastMessage.setText(conversation.getText());
        } else {
            holder.lastMessage.setText("No messages yet");
        }

        if (conversation.getReceiverImage() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(conversation.getReceiverImage())
                    .placeholder(R.drawable.baseline_person_24)
                    .into(holder.profileImage);
        }

        holder.itemView.setOnClickListener(v -> 
            listener.onConversationClick(conversation.getReceiverId())
        );
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView profileImage;
        TextView userName;
        TextView lastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.user_name);
            lastMessage = itemView.findViewById(R.id.last_message);
        }
    }
}