package com.example.trackmate.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.example.trackmate.R;
import com.example.trackmate.models.ReportedItem;
import com.example.trackmate.services.FirebaseService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.Calendar;

public class ReportFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ShapeableImageView itemImage;
    private TextInputEditText itemName;
    private TextInputEditText itemDate;
    private TextInputEditText itemLocation;
    private TextInputEditText itemDescription;
    private RadioGroup itemStatusGroup;
    private MaterialButton submitButton;
    private ProgressBar progressBar;
    private Uri imageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        itemImage = view.findViewById(R.id.item_image);
        itemName = view.findViewById(R.id.item_name);
        itemDate = view.findViewById(R.id.item_date);
        itemLocation = view.findViewById(R.id.item_location);
        itemDescription = view.findViewById(R.id.item_description);
        itemStatusGroup = view.findViewById(R.id.item_status_group);
        submitButton = view.findViewById(R.id.submit_button);
        progressBar = view.findViewById(R.id.progress_bar);

        itemImage.setOnClickListener(v -> openFileChooser());
        itemDate.setOnClickListener(v -> showDatePickerDialog());
        submitButton.setOnClickListener(v -> submitReport());

        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year1, month1, dayOfMonth) -> {
            String date = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
            itemDate.setText(date);
        }, year, month, day);
        datePickerDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            itemImage.setImageURI(imageUri);
        }
    }

    private void submitReport() {
        String name = itemName.getText().toString().trim();
        String date = itemDate.getText().toString().trim();
        String location = itemLocation.getText().toString().trim();
        String description = itemDescription.getText().toString().trim();
        String status = ((RadioButton) getView().findViewById(itemStatusGroup.getCheckedRadioButtonId())).getText().toString().trim();

        if (validateInputs(name, date, location, description, status)) {
            progressBar.setVisibility(View.VISIBLE);
            submitButton.setEnabled(false);

            if (imageUri != null) {
                // First upload the image, then create the report
                FirebaseService.uploadImage(imageUri, new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        String imageUrl = task.isSuccessful() && task.getResult() != null ? task.getResult().toString() : null;
                        createAndSaveReport(name, date, location, description, imageUrl, status);
                    }
                });
            } else {
                // Create report without image
                createAndSaveReport(name, date, location, description, null, status);
            }
        }
    }

    private boolean validateInputs(String name, String date, String location, String description, String status) {
        if (name.isEmpty()) {
            itemName.setError("Item Name is required");
            return false;
        }
        if (date.isEmpty()) {
            itemDate.setError("Date is required");
            return false;
        }
        if (location.isEmpty()) {
            itemLocation.setError("Location is required");
            return false;
        }
        if (description.isEmpty()) {
            itemDescription.setError("Description is required");
            return false;
        }
        if (status.isEmpty()) {
            Toast.makeText(getContext(), "Status is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void createAndSaveReport(String name, String date, String location, 
                                   String description, String imageUrl, String status) {
        String userId = FirebaseService.getCurrentUser().getUid();
        ReportedItem.Type type = status.equals("Lost") ? ReportedItem.Type.LOST : ReportedItem.Type.FOUND;
        ReportedItem item = new ReportedItem(name, description, location, 
                                           date, imageUrl, type);
        item.setUserId(userId);
        
        FirebaseService.reportItem(userId, item, task -> {
            progressBar.setVisibility(View.GONE);
            submitButton.setEnabled(true);
            
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Item reported successfully", 
                             Toast.LENGTH_SHORT).show();
                clearForm();
            } else {
                handleError("Failed to report item");
            }
        });
    }

    private void handleError(String message) {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void clearForm() {
        itemName.setText("");
        itemDate.setText("");
        itemLocation.setText("");
        itemDescription.setText("");
        itemStatusGroup.clearCheck();
        itemImage.setImageResource(R.drawable.baseline_add_a_photo_24);
        imageUri = null;
    }
}


