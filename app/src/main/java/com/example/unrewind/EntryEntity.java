package com.example.unrewind.data;

public class EntryEntity {

    public String entryId;
    public String songTitle;
    public String artist;
    public String notes;
    public String imageUrl;
    public long dateMillis;  // <<< MATCHES ADAPTER + FRAGMENT

    // Required empty constructor for Firestore
    public EntryEntity() {}

    public EntryEntity(String entryId, String songTitle, String artist,
                       String notes, String imageUrl, long dateMillis) {
        this.entryId = entryId;
        this.songTitle = songTitle;
        this.artist = artist;
        this.notes = notes;
        this.imageUrl = imageUrl;
        this.dateMillis = dateMillis;
    }
}
