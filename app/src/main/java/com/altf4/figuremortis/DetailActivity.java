package com.altf4.figuremortis;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.altf4.figuremortis.db.DatabaseHelper;
import com.altf4.figuremortis.service.GeminiService;

public class DetailActivity extends AppCompatActivity {

    private TextView tvName, tvBirth, tvDetails, tvSources;
    private ProgressBar progressBar;
    private Button btnSave;
    private GeminiService geminiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        tvName = findViewById(R.id.tv_name);
        tvBirth = findViewById(R.id.tv_birth);
        tvDetails = findViewById(R.id.tv_details);
        tvSources = findViewById(R.id.tv_sources);
        progressBar = findViewById(R.id.progress_bar_detail);
        btnSave = findViewById(R.id.btn_save);

        String apiKey = BuildConfig.GEMINI_API_KEY;
        geminiService = new GeminiService(apiKey);

        String personText = getIntent().getStringExtra("PERSON_TEXT");
        String personYear = getIntent().getStringExtra("PERSON_YEAR");

        if (personText != null && personYear != null) {
            String prompt = personText + " that was deceased in " + personYear;
            fetchBiography(prompt);
        } else {
            Toast.makeText(this, "Error: No data received.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchBiography(String prompt) {
        progressBar.setVisibility(View.VISIBLE);
        geminiService.getBio(prompt, new GeminiCallback() {
            @Override
            public void onSuccess(GeminiResponse response) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvName.setText(response.getName());
                    tvBirth.setText("Born: " + response.getBirth());
                    tvDetails.setText(response.getDetails());

                    StringBuilder sourcesText = new StringBuilder("Sources:\n");
                    if (response.getSources() != null) {
                        for (int i = 0; i < response.getSources().size(); i++) {
                            sourcesText.append(i + 1).append(". ").append(response.getSources().get(i).values().iterator().next()).append("\n");
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
