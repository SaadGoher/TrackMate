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

public class ReportedItemAdapter extends RecyclerView.Adapter<ReportedItemAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ReportedItem item);
    }

    private Context context;
    private List<ReportedItem> reportedItemList;
    private OnItemClickListener listener;

    public ReportedItemAdapter(Context context, List<ReportedItem> reportedItemList, OnItemClickListener listener) {
        this.context = context;
        this.reportedItemList = reportedItemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reported, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportedItem item = reportedItemList.get(position);
        holder.itemName.setText(item.getName());
        holder.itemDate.setText(item.getDate());
        holder.itemLocation.setText(item.getLocation());
        holder.itemTag.setText(item.getType() == ReportedItem.Type.LOST ? "Lost" : "Found");
        if (item.getImageUrl() != null) {
            Glide.with(context).load(item.getImageUrl()).into(holder.itemImage);
        } else {
            holder.itemImage.setImageResource(R.drawable.item);
        }
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return reportedItemList.size();
    }

    public void updateList(List<ReportedItem> newList) {
        reportedItemList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView itemImage;
        public TextView itemName;
        public TextView itemDate;
        public TextView itemLocation;
        public TextView itemTag;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemName = itemView.findViewById(R.id.item_name);
            itemDate = itemView.findViewById(R.id.item_date);
            itemLocation = itemView.findViewById(R.id.item_location);
            itemTag = itemView.findViewById(R.id.item_tag);
        }
    }
}

