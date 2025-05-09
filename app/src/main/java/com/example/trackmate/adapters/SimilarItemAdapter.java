package com.example.trackmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.models.ReportedItem;

import java.util.List;
import java.util.Map;

public class SimilarItemAdapter extends RecyclerView.Adapter<SimilarItemAdapter.ViewHolder> {

    public interface OnSimilarItemClickListener {
        void onSimilarItemClick(ReportedItem item);
    }

    private Context context;
    private List<ReportedItem> similarItems;
    private Map<String, Float> similarityScores;
    private OnSimilarItemClickListener listener;

    public SimilarItemAdapter(Context context, List<ReportedItem> similarItems, 
                              Map<String, Float> similarityScores, 
                              OnSimilarItemClickListener listener) {
        this.context = context;
        this.similarItems = similarItems;
        this.similarityScores = similarityScores;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_similar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportedItem item = similarItems.get(position);
        
        holder.itemName.setText(item.getName());
        holder.itemLocation.setText(item.getLocation());
        holder.itemDate.setText(item.getDate());
        
        // Format and display the similarity score as a percentage
        Float score = similarityScores.get(item.getId());
        if (score != null) {
            int percentage = Math.round(score * 100);
            
            // Color the match text based on the score
            if (percentage >= 80) {
                holder.itemMatch.setTextColor(context.getResources().getColor(R.color.colorSuccess));
                holder.itemMatch.setText(percentage + "% Match - Excellent");
            } else if (percentage >= 60) {
                holder.itemMatch.setTextColor(context.getResources().getColor(com.google.android.material.R.color.design_default_color_primary));
                holder.itemMatch.setText(percentage + "% Match - Good");
            } else {
                holder.itemMatch.setTextColor(context.getResources().getColor(R.color.colorWarnText));
                holder.itemMatch.setText(percentage + "% Match - Possible");
            }
        } else {
            holder.itemMatch.setVisibility(View.GONE);
        }
        
        // Load image with Glide
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.item)
                .into(holder.itemImage);
        } else {
            holder.itemImage.setImageResource(R.drawable.item);
        }
        
        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSimilarItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return similarItems.size();
    }

    public void updateItems(List<ReportedItem> items, Map<String, Float> scores) {
        this.similarItems = items;
        this.similarityScores = scores;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView itemImage;
        public TextView itemName, itemLocation, itemDate, itemMatch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.similar_item_image);
            itemName = itemView.findViewById(R.id.similar_item_name);
            itemLocation = itemView.findViewById(R.id.similar_item_location);
            itemDate = itemView.findViewById(R.id.similar_item_date);
            itemMatch = itemView.findViewById(R.id.similar_item_match);
        }
    }
}
