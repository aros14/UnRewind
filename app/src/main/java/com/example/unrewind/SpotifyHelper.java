package com.example.unrewind;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

// placeholder for the spotify helper that shows a track and will be updated as the Spotify API is finalized
public class SpotifyHelper {

    public interface SpotifyCallback {
        void onSuccess(Track track);
        void onFailure(String reason);
    }

    public static class Track {
        public String title;
        public String artist;
        public String album;
        public String albumArtUrl;
        public Track(String title, String artist, String album, String albumArtUrl) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.albumArtUrl = albumArtUrl;
        }
    }

    public static void fetchCurrentlyPlaying(Context ctx, SpotifyCallback callback) {
        // Mock network delay then return failure to trigger fallback
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // If you implement real Spotify, call callback.onSuccess(track) on success
            callback.onFailure("No Spotify credentials configured");
        }, 800);
    }

    public static Track getMockTrack() {
        return new Track(
                "Mockingbird Melody",
                "Sample Artist",
                "Mock Album",
                "" // leave empty to use placeholder art
        );
    }
}