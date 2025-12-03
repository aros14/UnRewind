package com.example.unrewind;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NewEntryActivity extends AppCompatActivity {

    private ImageView ivSongArt;
    private Button btnSaveEntry;
    private ImageButton ibSearchSong, ibPhotoSelect;
    private EditText etNotes;
    private ProgressBar progressBar;
    private TextView tvSongTitle, tvSongArtist;

    private Uri selectedImageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private final long MAX_FILE_BYTES = 5L * 1024L * 1024L; // 5 MB

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_entry);

        ivSongArt = findViewById(R.id.ivSongArt);
        ibPhotoSelect = findViewById(R.id.ibPhotoSelect);
        btnSaveEntry = findViewById(R.id.btnSaveEntry);
        ibSearchSong = findViewById(R.id.ibSearchSong);
        etNotes = findViewById(R.id.etNotes);
        progressBar = findViewById(R.id.progressBar);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Image picker
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            selectedImageUri = uri;
                            ivSongArt.setImageURI(uri); // show selected image
                        }
                    }
                });

        // Permission request
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openImagePicker();
                    else Toast.makeText(this, "Permission required to pick images", Toast.LENGTH_SHORT).show();
                });

        ibPhotoSelect.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else openImagePicker();
        });

        // Mock song search
        ibSearchSong.setOnClickListener(v -> {
            tvSongTitle.setText("Mockingbird Melody");
            tvSongArtist.setText("Sample Artist");
        });

        btnSaveEntry.setOnClickListener(v -> saveEntry());
    }

    private void openImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void saveEntry() {
        String notes = etNotes.getText().toString().trim();
        String songTitle = tvSongTitle.getText().toString();
        String artist = tvSongArtist.getText().toString();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be signed in to save entries", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);

        String uid = mAuth.getCurrentUser().getUid();
        String entryId = UUID.randomUUID().toString(); // can also use Firestore auto ID
        long now = System.currentTimeMillis();

        Map<String, Object> entryData = new HashMap<>();
        entryData.put("entryId", entryId);
        entryData.put("userId", uid);
        entryData.put("songTitle", songTitle);
        entryData.put("artist", artist);
        entryData.put("notes", notes);
        entryData.put("createdAt", now);
        entryData.put("imageUrl", null);

        if (selectedImageUri != null) {
            try {
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
                byte[] imageData = new byte[is.available()];
                is.read(imageData);

                if (imageData.length > MAX_FILE_BYTES) {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(this, "Image too large. Choose a smaller image.", Toast.LENGTH_LONG).show();
                    return;
                }

                String ext = getFileExtension(selectedImageUri);
                StorageReference imgRef = storageRef.child("images/" + uid + "/" + entryId + "." + ext);
                imgRef.putBytes(imageData)
                        .addOnSuccessListener(taskSnapshot -> imgRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    entryData.put("imageUrl", uri.toString());
                                    saveToFirestore(uid, entryId, entryData);
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(ProgressBar.GONE);
                                    Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }))
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(ProgressBar.GONE);
                            Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } catch (Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(this, "Failed to read image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            saveToFirestore(uid, entryId, entryData);
        }
    }

    private void saveToFirestore(String uid, String entryId, Map<String, Object> entryData) {
        db.collection("entries")
                .document(entryId)
                .set(entryData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(NewEntryActivity.this, "Entry saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(NewEntryActivity.this, "Failed to save entry: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }
}
