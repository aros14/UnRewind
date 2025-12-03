package com.example.unrewind;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JournalActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        viewPager = findViewById(R.id.viewPager);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        adapter = new EntryPagerAdapter(this, entries);
        viewPager.setAdapter(adapter);

        // loading entries
        loadEntries();

        // If launched with openEntryId, scroll to that entry after load
    }

    private void loadEntries() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Sign in to view your journal", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        // Load local entries on background thread
        new Thread(() -> {
            List<EntryEntity> local = entryDao.getAllForUser(userId);
            if (local != null && !local.isEmpty()) {
                // Order oldest to newest
                Collections.sort(local, (a, b) -> Long.compare(a.createdAt, b.createdAt));
                runOnUiThread(() -> populatePager(local));
            } else {
                // No local entries — try Firestore
                firestore.collection("entries")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            List<EntryEntity> remote = new ArrayList<>();
                            for (var doc : querySnapshot.getDocuments()) {
                                EntryEntity e = doc.toObject(EntryEntity.class);
                                if (e != null) remote.add(e);
                            }
                            if (!remote.isEmpty()) {
                                // Save remote entries locally for offline use
                                new Thread(() -> {
                                    for (EntryEntity e : remote) entryDao.insert(e);
                                }).start();
                                Collections.sort(remote, (a, b) -> Long.compare(a.createdAt, b.createdAt));
                                runOnUiThread(() -> populatePager(remote));
                            } else {
                                runOnUiThread(() -> Toast.makeText(JournalActivity.this, "No journal entries yet", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .addOnFailureListener(e -> runOnUiThread(() -> Toast.makeText(JournalActivity.this, "Failed to load entries", Toast.LENGTH_SHORT).show()));
            }
        }).start();
    }

    private void populatePager(List<EntryEntity> list) {
        entries.clear();
        entries.addAll(list);
        adapter.notifyDataSetChanged();

        // If an entryId was provided, jump to its index
        Intent intent = getIntent();
        String openEntryId = intent != null ? intent.getStringExtra("openEntryId") : null;
        if (openEntryId != null) {
            int idx = 0;
            for (int i = 0; i < entries.size(); i++) {
                if (openEntryId.equals(entries.get(i).entryId)) {
                    idx = i;
                    break;
                }
            }
            viewPager.setCurrentItem(idx, false);
        }
    }
}