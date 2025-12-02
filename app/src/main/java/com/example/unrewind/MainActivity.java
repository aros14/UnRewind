package com.example.unrewind;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
//import com.bumptech.glide.Glide;

// The home screen/dashboard that is meant to show the currently playing track, date, and then 2 quick action buttons

public class MainActivity extends AppCompatActivity {

    private TextView tvSongTitle, tvArtist, tvDateSnippet;
    private ImageView ivAlbumArt;
    private ProgressBar progressBar;
    private Button btnCalendar, btnLog, btnJournal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvArtist = findViewById(R.id.tvArtist);
        ivAlbumArt = findViewById(R.id.ivAlbumArt);
        progressBar = findViewById(R.id.progressBar);
        tvDateSnippet = findViewById(R.id.tvDateSnippet);

        btnCalendar = findViewById(R.id.btnCalendar);
        btnLog = findViewById(R.id.btnLog);
        btnJournal = findViewById(R.id.btnJournal);

        btnCalendar.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CalendarActivity.class)));
        btnLog.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NewEntryActivity.class)));
        btnJournal.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, JournalActivity.class)));

        // Show today's date snippet
       // tvDateSnippet.setText(Utils.getTodayFormatted()); // Utils helper shown below

        // Load currently playing track (attempt Spotify, fallback to mock)
        loadCurrentlyPlayingTrack();
    }

    private void loadCurrentlyPlayingTrack() {
        progressBar.setVisibility(View.VISIBLE);

        // Attempt to fetch from Spotify via SpotifyHelper (placeholder)
        SpotifyHelper.fetchCurrentlyPlaying(this, new SpotifyHelper.SpotifyCallback() {
            @Override
            public void onSuccess(SpotifyHelper.Track track) {
                progressBar.setVisibility(View.GONE);
                displayTrack(track);
            }

            @Override
            public void onFailure(String reason) {
                // Fallback to mocked track
                progressBar.setVisibility(View.GONE);
                SpotifyHelper.Track mock = SpotifyHelper.getMockTrack();
                displayTrack(mock);
            }
        });
    }

   private void displayTrack(SpotifyHelper.Track track) {
        tvSongTitle.setText(track.title);
        tvArtist.setText(track.artist);
        if (track.albumArtUrl != null && !track.albumArtUrl.isEmpty()) {
            //Glide.with(this).load(track.albumArtUrl).centerCrop().into(ivAlbumArt);
        } else {
            ivAlbumArt.setImageResource(R.drawable.ic_music_placeholder);
        }

    }
}

