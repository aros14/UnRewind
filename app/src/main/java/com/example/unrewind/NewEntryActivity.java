package com.example.unrewind;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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

import java.util.UUID;

public class NewEntryActivity extends AppCompatActivity {

    private ImageView ivSongArt;
    private ImageButton ibPhotoSelect, ibSearchSong;
    private Button btnSaveEntry;
    private EditText etNotes;
    private ProgressBar progressBar;
    private TextView tvSongTitle, tvSongArtist;

    private Uri selectedImageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_entry);

        // Initialize views
        ivSongArt = findViewById(R.id.ivSongArt);
        ibPhotoSelect = findViewById(R.id.ibPhotoSelect);
        btnSaveEntry = findViewById(R.id.btnSaveEntry);
        ibSearchSong = findViewById(R.id.ibSearchSong);
        etNotes = findViewById(R.id.etNotes);
        //progressBar = findViewById(R.id.progressBar);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Setup ActivityResultLaunchers
        setupResultLaunchers();

        // Set click listeners
        ibPhotoSelect.setOnClickListener(v -> handleImageSelection());
        ibSearchSong.setOnClickListener(v -> {
            // Mock song search - replace with your actual implementation
            tvSongTitle.setText("song title");
            tvSongArtist.setText("artist");
        });
        btnSaveEntry.setOnClickListener(v -> saveEntry());
    }

    private void setupResultLaunchers() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                    }
                });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openImagePicker();
                    else Toast.makeText(this, "Permission required to select images", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleImageSelection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void saveEntry() {
        String notes = etNotes.getText().toString().trim();
        String songTitle = tvSongTitle.getText().toString();
        String artist = tvSongArtist.getText().toString();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to save an entry", Toast.LENGTH_SHORT).show();
            return;
        }

        if (songTitle.isEmpty() || artist.isEmpty()) {
            Toast.makeText(this, "Please select a song before saving", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String uid = mAuth.getCurrentUser().getUid();
        long now = System.currentTimeMillis();

        EntryEntity newEntry = new EntryEntity(uid, songTitle, artist, notes, null, now);

        if (selectedImageUri != null) {
            String extension = getFileExtension(selectedImageUri);
            StorageReference imgRef = storageRef.child("images/" + uid + "/" + UUID.randomUUID().toString() + "." + extension);

            imgRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> imgRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                newEntry.imageUrl = uri.toString();
                                saveEntryToFirestore(newEntry);
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(NewEntryActivity.this, "Failed to get image URL", Toast.LENGTH_LONG).show();
                            }))
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NewEntryActivity.this, "Image upload failed", Toast.LENGTH_LONG).show();
                    });
        } else {
            saveEntryToFirestore(newEntry);
        }
    }

    private void saveEntryToFirestore(EntryEntity entry) {
        db.collection("entries")
                .add(entry)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NewEntryActivity.this, "Entry saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NewEntryActivity.this, "Failed to save entry", Toast.LENGTH_LONG).show();
                });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }
}
