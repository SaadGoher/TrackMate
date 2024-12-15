package com.example.trackmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.models.ReportedItem;
import com.google.android.material.button.MaterialButton;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView itemImage;
    private TextView itemName, itemDate, itemLocation, itemDescription;
    private MaterialButton callButton, messageButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        itemImage = findViewById(R.id.item_image);
        itemName = findViewById(R.id.item_name);
        itemDate = findViewById(R.id.item_date);
        itemLocation = findViewById(R.id.item_location);
        itemDescription = findViewById(R.id.item_description);
        callButton = findViewById(R.id.call_button);
        messageButton = findViewById(R.id.message_button);

        ReportedItem item = (ReportedItem) getIntent().getSerializableExtra("item");

        if (item != null) {
            itemName.setText(item.getName());
            itemDate.setText(item.getDate());
            itemLocation.setText(item.getLocation());
            itemDescription.setText(item.getDescription());
            if (item.getImageUrl() != null) {
                Glide.with(this).load(item.getImageUrl()).into(itemImage);
            } else {
                itemImage.setImageResource(R.drawable.item);
            }

            callButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + item.getUserId())); // Assuming userId is the phone number
                startActivity(intent);
            });

            messageButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("smsto:" + item.getUserId())); // Assuming userId is the phone number
                startActivity(intent);
            });
        }
    }
}
    
