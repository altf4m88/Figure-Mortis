package com.altf4.figuremortis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements HistoryAdapter.OnClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        titleTextView = findViewById(R.id.titleTextView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set the current date in the title
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(calendar.getTime());
        titleTextView.setText("Today is " + formattedDate + ", \n I wonder who died...");

        findViewById(R.id.btn_saved).setOnClickListener(v -> {
            startActivity(new Intent(this, SavedActivity.class));
        });

        fetchData();
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.loadingImage).setVisibility(View.VISIBLE);
        findViewById(R.id.loadingText).setVisibility(View.VISIBLE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://history.muffinlabs.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HistoryApiService apiService = retrofit.create(HistoryApiService.class);

        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        Call<HistoryResponse> call = apiService.getEvents(month, day);
        call.enqueue(new Callback<HistoryResponse>() {
            @Override
            public void onResponse(Call<HistoryResponse> call, Response<HistoryResponse> response) {
                progressBar.setVisibility(View.GONE);
                findViewById(R.id.loadingImage).setVisibility(View.GONE);
                findViewById(R.id.loadingText).setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Death> deaths = response.body().getData().getDeaths();

                    // Sort deaths by year
                    Collections.sort(deaths, new Comparator<Death>() {
                        @Override
                        public int compare(Death d1, Death d2) {
                            return Integer.compare(Integer.parseInt(d1.getYear()), Integer.parseInt(d2.getYear()));
                        }
                    });

                    // Group deaths by year
                    Map<String, List<Death>> deathsByYear = new LinkedHashMap<>();
                    for (Death death : deaths) {
                        String year = death.getYear();
                        if (!deathsByYear.containsKey(year)) {
                            deathsByYear.put(year, new ArrayList<>());
                        }
                        deathsByYear.get(year).add(death);
                    }

                    // Create DisplayItems
                    List<DisplayItem> displayItems = new ArrayList<>();
                    for (Map.Entry<String, List<Death>> entry : deathsByYear.entrySet()) {
                        displayItems.add(new DisplayItem(DisplayItem.TYPE_HEADER, entry.getKey()));
                        for (Death death : entry.getValue()) {
                            displayItems.add(new DisplayItem(DisplayItem.TYPE_DEATH, death));
                        }
                    }

                    HistoryAdapter adapter = new HistoryAdapter(displayItems, MainActivity.this);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<HistoryResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                findViewById(R.id.loadingImage).setVisibility(View.GONE);
                findViewById(R.id.loadingText).setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(Death death) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("PERSON_TEXT", death.getText());
        intent.putExtra("PERSON_YEAR", death.getYear());
        startActivity(intent);
    }
}
