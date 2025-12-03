package com.example.unrewind;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class CalendarActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener {

    private TextView monthYearText;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;

    private CardView entryCard;
    private ImageView ivSongArt;
    private TextView tvSongTitle, tvSongArtist, tvJournalEntry;
    private Button btnLogSong;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        initWidgets();
        selectedDate = LocalDate.now();
        setMonthView();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initWidgets() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        monthYearText = findViewById(R.id.tvMonth);

        entryCard = findViewById(R.id.entryCard);
        ivSongArt = findViewById(R.id.ivSongArt);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);
        tvJournalEntry = findViewById(R.id.tvJournalEntry);
        btnLogSong = findViewById(R.id.btnLogSong);
    }

    private void setMonthView() {
        monthYearText.setText(monthYearFromDate(selectedDate));
        ArrayList<String> daysInMonth = daysInMonthArray(selectedDate);

        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);
    }

    private ArrayList<String> daysInMonthArray(LocalDate date) {
        ArrayList<String> daysInMonthArray = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate firstOfMonth = selectedDate.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();

        for (int i = 1; i <= 42; i++) {
            if (i <= dayOfWeek || i > daysInMonth + dayOfWeek) {
                daysInMonthArray.add("");
            } else {
                daysInMonthArray.add(String.valueOf(i - dayOfWeek));
            }
        }
        return daysInMonthArray;
    }

    private String monthYearFromDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        return date.format(formatter);
    }

    public void previousMonthAction(View view) {
        selectedDate = selectedDate.minusMonths(1);
        setMonthView();
    }

    public void nextMonthAction(View view) {
        selectedDate = selectedDate.plusMonths(1);
        setMonthView();
    }

    @Override
    public void onItemClick(int position, String dayText) {
        if (!dayText.equals("")) {
            int day = Integer.parseInt(dayText);
            LocalDate clickedDate = selectedDate.withDayOfMonth(day);

            if (mAuth.getCurrentUser() != null) {
                fetchEntryForDate(clickedDate);
            } else {
                Toast.makeText(this, "Please log in to view entries", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchEntryForDate(LocalDate date) {
        long startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("entries")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("dateMillis", startOfDay)
                .whereLessThan("dateMillis", endOfDay)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Entry exists
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            EntryEntity entry = document.toObject(EntryEntity.class);
                            displayEntry(entry);
                            break; // Should only be one per day
                        }
                    } else {
                        // No entry
                        displayNoEntry(date);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch entries", Toast.LENGTH_SHORT).show();
                    displayNoEntry(date);
                });
    }

    private void displayEntry(EntryEntity entry) {
        entryCard.setVisibility(View.VISIBLE);
        btnLogSong.setVisibility(View.GONE);

        tvSongTitle.setText(entry.songTitle);
        tvSongArtist.setText(entry.artist);
        tvJournalEntry.setText(entry.notes);
        // Load image with Glide or another library if you have one
    }

    private void displayNoEntry(LocalDate date) {
        entryCard.setVisibility(View.GONE);
        btnLogSong.setVisibility(View.VISIBLE);
        btnLogSong.setOnClickListener(v -> {
            Intent intent = new Intent(this, NewEntryActivity.class);
            intent.putExtra("dateMillis", date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
            startActivity(intent);
        });
    }
}
