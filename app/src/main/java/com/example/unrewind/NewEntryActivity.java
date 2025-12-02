package com.example.unrewind;

import androidx.appcompat.app.AppCompatActivity;
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
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.util.UUID;

//set up firebase logic and the imports:
//sample create entity class: import com.example.unrewind.data.EntryEntity;
//sample create entity class: import com.example.unrewind.data.EntryViewModel;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;

public class NewEntryActivity extends AppCompatActivity {
    private ImageView ivPreview, ivSongArt;
    private Button btnSaveEntry;
    private ImageButton ibSearchSong, ibPhotoSelect;
    private EditText etNotes;
    private ProgressBar progressBar;
    private TextView tvSongTitle, tvSongArtist;

    private Uri selectedImageUri;
    private EntryViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private StorageReference storageRef;

    private final long MAX_FILE_BYTES = 5L * 1024L * 1024L; // 5 MB

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_entry);

        //ivPreview = findViewById(R.id.ivPreview); do we want to display the photo they pick in this activity?
        ivSongArt = findViewById(R.id.ivSongArt);
        ibPhotoSelect = findViewById(R.id.ibPhotoSelect);
        btnSaveEntry = findViewById(R.id.btnSaveEntry);
        ibSearchSong = findViewById(R.id.ibSearchSong);
        etNotes = findViewById(R.id.etNotes);
        progressBar = findViewById(R.id.progressBar);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);

        viewModel = new EntryViewModel(getApplication());
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            selectedImageUri = uri;
                            Glide.with(NewEntryActivity.this).load(uri).centerCrop().into(ivPreview);
                        }
                    }
                });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(this, "Permission required to pick images", Toast.LENGTH_SHORT).show();
                    }
                });

        ibPhotoSelect.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker();
            }
        });

        ibSearchSong.setOnClickListener(v -> {
            // try to get spotify integration to look for songs, unsure if the library will be imported or if we need to find another one
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

        progressBar.setVisibility(View.VISIBLE);
        String entryId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        // checking if an image selected, compressing it, making sure its under 5MB and then uploading it
        if (selectedImageUri != null) {
            try {
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
                byte[] compressed = PicCompressor.compressToJpeg(this, selectedImageUri, 1024, 80);
                if (compressed.length > MAX_FILE_BYTES) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Image too large after compression. Choose a smaller image.", Toast.LENGTH_LONG).show();
                    return;
                }
                String ext = getFileExtension(selectedImageUri);
                StorageReference imgRef = storageRef.child("images/" + mAuth.getCurrentUser().getUid() + "/" + entryId + "." + ext);
                imgRef.putBytes(compressed)
                        .addOnSuccessListener(taskSnapshot -> imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveEntryToLocalAndRemote(entryId, songTitle, artist, notes, imageUrl, now);
                        }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(NewEntryActivity.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(NewEntryActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } catch (Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to read image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            // saving with no picture
            saveEntryToLocalAndRemote(entryId, songTitle, artist, notes, null, now);
        }
    }

    // tweak to fit firebase logic and database schema that you set up
    private void saveEntryToLocalAndRemote(String entryId, String songTitle, String artist,
                                           String notes, String imageUrl, long now) {
        String userId = mAuth.getCurrentUser().getUid();
        EntryEntity entry = new EntryEntity(entryId, userId, now, songTitle, artist, null, null, notes, imageUrl, now, false);

        // Save locally immediately?
        viewModel.saveEntryLocally(entry);

        // Save to Firestore?
        firestore.collection("entries").document(entryId)
                .set(entry)
                .addOnSuccessListener(aVoid -> {
                    // Mark synced in local DB via WorkManager or direct update
                    // For simplicity, update synced flag here by re-inserting with synced = true
                    entry.synced = true;
                    viewModel.saveEntryLocally(entry);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NewEntryActivity.this, "Entry saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Keep local copy unsynced; WorkManager will retry
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NewEntryActivity.this, "Saved locally. Will sync when online.", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }

}
