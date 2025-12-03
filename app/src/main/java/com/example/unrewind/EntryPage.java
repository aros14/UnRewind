package com.example.unrewind;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class EntryFragment extends Fragment {

    /* variables from database to display details on the page
    private static final String ARG_ENTRY_ID = "arg_entry_id";
    private static final String ARG_SONG_TITLE = "arg_song_title";
    private static final String ARG_ARTIST = "arg_artist";
    private static final String ARG_NOTES = "arg_notes";
    private static final String ARG_IMAGE_URL = "arg_image_url";
    private static final String ARG_DATE = "arg_date"; */

    public static EntryFragment newInstance(String entryId, String songTitle, String artist,
                                            String notes, String imageUrl, long dateMillis) {
        EntryFragment f = new EntryFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ENTRY_ID, entryId);
        b.putString(ARG_SONG_TITLE, songTitle);
        b.putString(ARG_ARTIST, artist);
        b.putString(ARG_NOTES, notes);
        b.putString(ARG_IMAGE_URL, imageUrl);
        b.putLong(ARG_DATE, dateMillis);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entry_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView tvDate = view.findViewById(R.id.tvEntryDate);
        TextView tvSong = view.findViewById(R.id.tvEntrySongTitle);
        TextView tvArtist = view.findViewById(R.id.tvEntryArtist);
        TextView tvNotes = view.findViewById(R.id.tvEntryNotes);
        ImageView ivArt = view.findViewById(R.id.ivEntryAlbumArt);

        Bundle args = getArguments();
        if (args == null) return;

        String songTitle = args.getString(ARG_SONG_TITLE, "No song");
        String artist = args.getString(ARG_ARTIST, "");
        String notes = args.getString(ARG_NOTES, "");
        String imageUrl = args.getString(ARG_IMAGE_URL, "");
        long dateMillis = args.getLong(ARG_DATE, System.currentTimeMillis());

        tvDate.setText(Utils.formatDateForDisplay(dateMillis));
        tvSong.setText(songTitle);
        tvArtist.setText(artist);
        tvNotes.setText(notes != null && !notes.isEmpty() ? notes : "—");

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext()).load(imageUrl).centerCrop().into(ivArt);
        } else {
            ivArt.setImageResource(R.drawable.ic_music_placeholder);
        }
    }
}