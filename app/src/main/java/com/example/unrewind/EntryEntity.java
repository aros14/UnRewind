package com.example.unrewind;

import com.google.firebase.firestore.DocumentId;

public class EntryEntity {

    @DocumentId
    public String entryId;
    public String userId;
    public String songTitle;
    public String artist;
    public String notes;
    public String imageUrl;
    public long dateMillis;

    // Required empty constructor for Firestore
    public EntryEntity() {}

    public EntryEntity(String userId, String songTitle, String artist,
                       String notes, String imageUrl, long dateMillis) {
        this.userId = userId;
        this.songTitle = songTitle;
        this.artist = artist;
        this.notes = notes;
        this.imageUrl = imageUrl;
        this.dateMillis = dateMillis;
    }
}
