package com.example.trackmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.example.trackmate.R;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.services.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView itemImage;
    private TextView itemName, itemDate, itemLocation, itemDescription;
    private MaterialButton callButton, messageButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Item Details");
        }

        itemImage = findViewById(R.id.item_image);
        itemName = findViewById(R.id.item_name);
        itemDate = findViewById(R.id.item_date);
        itemLocation = findViewById(R.id.item_location);
        itemDescription = findViewById(R.id.item_description);
        callButton = findViewById(R.id.call_button);
        messageButton = findViewById(R.id.message_button);

        String itemId = getIntent().getStringExtra("item_id");
        if (itemId != null) {
            loadItemDetails(itemId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadItemDetails(String itemId) {
        DatabaseReference itemRef = FirebaseService.getDatabase().child("reported_items").child(itemId);
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReportedItem item = snapshot.getValue(ReportedItem.class);
                if (item != null) {
                    item.setId(snapshot.getKey());
                    displayItemDetails(item);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void displayItemDetails(ReportedItem item) {
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
            Intent intent = new Intent(this, MessagesActivity.class);
            intent.putExtra("receiver_id", item.getUserId());
            startActivity(intent);
        });
    }
}

