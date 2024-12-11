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

    private Context context;
    private List<ReportedItem> reportedItemList;

    public ReportedItemAdapter(Context context, List<ReportedItem> reportedItemList) {
        this.context = context;
        this.reportedItemList = reportedItemList;
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
        holder.itemDescription.setText(item.getDescription());
        Glide.with(context).load(item.getImageUrl()).into(holder.itemImage);
    }

    @Override
    public int getItemCount() {
        return reportedItemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView itemImage;
        public TextView itemName;
        public TextView itemDate;
        public TextView itemLocation;
        public TextView itemDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemName = itemView.findViewById(R.id.item_name);
            itemDate = itemView.findViewById(R.id.item_date);
            itemLocation = itemView.findViewById(R.id.item_location);
            itemDescription = itemView.findViewById(R.id.item_description);
        }
    }
}

