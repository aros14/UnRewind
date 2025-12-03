package com.example.unrewind;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class EntryPagerAdapter extends FragmentStateAdapter {

    private final List<EntryEntity> entries;

    public EntryPagerAdapter(@NonNull FragmentActivity fa, List<EntryEntity> entries) {
        super(fa);
        this.entries = entries;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        EntryEntity e = entries.get(position);
        return EntryFragment.newInstance(
                e.entryId,
                e.songTitle != null ? e.songTitle : "",
                e.artist != null ? e.artist : "",
                e.notes != null ? e.notes : "",
                e.imageUrl != null ? e.imageUrl : "",
                e.dateMillis
        );
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }
}
