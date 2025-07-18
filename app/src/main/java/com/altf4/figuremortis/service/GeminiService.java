package com.altf4.figuremortis.service;

import com.altf4.figuremortis.GeminiCallback;
import com.altf4.figuremortis.GeminiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;

public class GeminiService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final GenerativeModelFutures model;
    private final Gson gson = new Gson();

    public GeminiService(String apiKey) {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.2f;
//        configBuilder.responseMimeType = "application/json";

        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", apiKey, configBuilder.build());
        model = GenerativeModelFutures.from(gm);
    }

    public void getBio(String personInfo, GeminiCallback callback) {
        executor.execute(() -> {
            try {
                Content systemInstruction = new Content.Builder().addText(
                        "You are an expert historian who is capable of finding details about a historical figure from their name and the date of their death. You must use grounding search tool to verify the information. Always return the response with this JSON format:\n\n{\n\"name\" : \"Full name of the historical figure\",\n\"birth\" : \"Birth date if there's any information\",\n\"details\": \"Detailed 3-paragraph biography about the person (clean format without source annotation)\",\n\"sources\": [{\"source number\": \"Links of the information sources\"}]\n}"
                ).build();

                Content userContent = new Content.Builder()
                        .addText("Who was " + personInfo)
                        .build();

                GenerateContentResponse response = model.generateContent(userContent).get();
                String jsonResponse = response.getText().replace("```json", "").replace("```", "").trim();
                GeminiResponse geminiResponse = gson.fromJson(jsonResponse, GeminiResponse.class);

                if (geminiResponse != null) {
                    callback.onSuccess(geminiResponse);
                } else {
                    Log.e("GeminiService", "Failed to parse JSON response: " + jsonResponse);
                    callback.onFailure(new Exception("Failed to parse JSON response."));
                }
            } catch (Exception e) {
                Log.e("GeminiService", "Error in getBio: " + e.getMessage(), e);
                callback.onFailure(e);
            }
        });
    }
}
