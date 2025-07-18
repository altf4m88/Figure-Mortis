package com.altf4.figuremortis.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.altf4.figuremortis.GeminiResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mortis.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_SAVED_FIGURES = "saved_figures";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_BIRTH_DATE = "birth_date";
    private static final String COLUMN_DEATH_YEAR = "death_year";
    private static final String COLUMN_DETAILS = "details";
    private static final String COLUMN_SOURCES = "sources";

    private static final String CREATE_TABLE_SAVED_FIGURES = "CREATE TABLE " + TABLE_SAVED_FIGURES + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_NAME + " TEXT NOT NULL," +
            COLUMN_BIRTH_DATE + " TEXT," +
            COLUMN_DEATH_YEAR + " TEXT," +
            COLUMN_DETAILS + " TEXT NOT NULL," +
            COLUMN_SOURCES + " TEXT" +
            ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SAVED_FIGURES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAVED_FIGURES);
        onCreate(db);
    }

    public void addFigure(GeminiResponse response, String deathYear) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, response.getName());
        values.put(COLUMN_BIRTH_DATE, response.getBirth());
        values.put(COLUMN_DEATH_YEAR, deathYear);
        values.put(COLUMN_DETAILS, response.getDetails());
        values.put(COLUMN_SOURCES, new Gson().toJson(response.getSources()));
        db.insert(TABLE_SAVED_FIGURES, null, values);
        db.close();
    }

    public GeminiResponse getFigure(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SAVED_FIGURES, new String[]{COLUMN_ID, COLUMN_NAME, COLUMN_BIRTH_DATE, COLUMN_DEATH_YEAR, COLUMN_DETAILS, COLUMN_SOURCES},
                COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        GeminiResponse response = new GeminiResponse();
        response.setName(cursor.getString(1));
        response.setBirth(cursor.getString(2));
        response.setDetails(cursor.getString(4));
        // You'll need to deserialize the sources from JSON here

        cursor.close();
        return response;
    }

    public List<GeminiResponse> getAllFigures() {
        List<GeminiResponse> figureList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_SAVED_FIGURES;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                GeminiResponse response = new GeminiResponse();
                response.setName(cursor.getString(1));
                response.setBirth(cursor.getString(2));
                response.setDetails(cursor.getString(4));
                // You'll need to deserialize the sources from JSON here
                figureList.add(response);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return figureList;
    }

    public void deleteFigure(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SAVED_FIGURES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
