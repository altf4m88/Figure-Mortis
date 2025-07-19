package com.altf4.figuremortis;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.altf4.figuremortis.db.DatabaseHelper;
import com.altf4.figuremortis.service.GeminiService;
import com.altf4.figuremortis.service.GeminiService.GroundedResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

public class DetailActivity extends AppCompatActivity {

    private TextView tvName, tvBirth, tvDetails, tvSources, tvTitleSelected;
    private ProgressBar progressBar;
    private ImageButton btnSave;
    private GeminiService geminiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        tvTitleSelected = findViewById(R.id.tv_title_selected);
        tvName = findViewById(R.id.tv_name);
        tvBirth = findViewById(R.id.tv_birth);
        tvDetails = findViewById(R.id.tv_details);
        tvSources = findViewById(R.id.tv_sources);
        progressBar = findViewById(R.id.progress_bar_detail);
        btnSave = findViewById(R.id.btn_save);

        geminiService = new GeminiService(BuildConfig.GEMINI_API_KEY);

        String personText = getIntent().getStringExtra("PERSON_TEXT");
        String personYear = getIntent().getStringExtra("PERSON_YEAR");
        String personBirth = getIntent().getStringExtra("PERSON_BIRTH");
        String personDetails = getIntent().getStringExtra("PERSON_DETAILS");
        String personSources = getIntent().getStringExtra("PERSON_SOURCES");

        if (personDetails != null) {
            // Data is pre-fetched from SavedActivity
            tvTitleSelected.setText("Ah... you just selected...");
            tvName.setText(personText);
            tvBirth.setText("Born: " + personBirth);
            tvDetails.setText(personDetails);

            StringBuilder sourcesText = new StringBuilder("Sources:\n");
            if (personSources != null && !personSources.isEmpty()) {
                List<Map<String, String>> sources = new Gson().fromJson(personSources, new TypeToken<List<Map<String, String>>>(){}.getType());
                for (Map<String, String> sourceMap : sources) {
                    for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
                        sourcesText.append(entry.getKey()).append(". ").append(entry.getValue()).append("\n");
                    }
                }
            }
            tvSources.setText(sourcesText.toString());

            btnSave.setEnabled(false); // Already saved or no need to save again
            progressBar.setVisibility(View.GONE);

        } else if (personText != null && personYear != null) {
            // Fetch data from GeminiService
            String prompt = personText + " that was deceased in " + personYear;
            fetchBiography(prompt);
        } else {
            Toast.makeText(this, "Error: No data received.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void fetchBiography(String prompt) {
        progressBar.setVisibility(View.VISIBLE);
        geminiService.generateGroundedResponse(prompt, new GeminiService.GeminiCallback() {

            @Override
            public void onComplete(GeminiService.GroundedResponse response) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvName.setText(response.name);
                    tvBirth.setText("Born: " + response.birth);
                    tvDetails.setText(response.details);

                    StringBuilder sourcesText = new StringBuilder("Sources:\n");
                    if (response.sources != null) {
                        for (Map<String, String> sourceMap : response.sources) {
                            for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
                                sourcesText.append(entry.getKey()).append(". ").append(entry.getValue()).append("\n");
                            }
                        }
                    }
                    tvSources.setText(sourcesText.toString());

                    btnSave.setEnabled(true);

                    btnSave.setOnClickListener(v -> {
                        DatabaseHelper db = new DatabaseHelper(DetailActivity.this);
                        String personYear = getIntent().getStringExtra("PERSON_YEAR");
                        db.addFigure(response, personYear);
                        Toast.makeText(DetailActivity.this, "Figure saved", Toast.LENGTH_SHORT).show();
                    });
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(DetailActivity.this, "Failed to load biography: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
