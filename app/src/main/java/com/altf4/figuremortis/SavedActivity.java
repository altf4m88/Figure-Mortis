package com.altf4.figuremortis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.altf4.figuremortis.db.DatabaseHelper;
import com.altf4.figuremortis.service.GeminiService;
import com.google.gson.Gson;

import java.util.List;

public class SavedActivity extends AppCompatActivity implements SavedFiguresAdapter.OnClickListener {

    private RecyclerView recyclerView;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved);

        recyclerView = findViewById(R.id.savedRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = new DatabaseHelper(this);
        List<GeminiService.GroundedResponse> figures = db.getAllFigures();

        if (figures.isEmpty()) {
            findViewById(R.id.emptyImage).setVisibility(View.VISIBLE);
            findViewById(R.id.emptyText).setVisibility(View.VISIBLE);
        } else {
            SavedFiguresAdapter adapter = new SavedFiguresAdapter(figures, this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onItemClick(GeminiService.GroundedResponse figure) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("PERSON_TEXT", figure.name);
        intent.putExtra("PERSON_BIRTH", figure.birth);
        intent.putExtra("PERSON_DETAILS", figure.details);
        intent.putExtra("PERSON_SOURCES", new Gson().toJson(figure.sources));
        startActivity(intent);
    }
}
