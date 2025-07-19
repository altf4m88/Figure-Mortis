package com.altf4.figuremortis.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A service to interact with the Google Gemini API using the 'generateContent' method.
 * This service is designed for "grounded" generation, where the model's response is
 * grounded by Google Search results. It uses a few-shot prompting technique to guide
 * the model's output format into a single JSON response.
 */
public class GeminiService {

    private final String TAG = "GeminiService";

    // Store the API Key securely, not hardcoded in production.
    private final String geminiApiKey;
    private static final String MODEL_ID = "gemini-2.0-flash";
    private static final String API_METHOD = "generateContent";
    private final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_ID;

    private final OkHttpClient httpClient;
    private final Gson gson;

    /**
     * Callback interface for handling the response from the Gemini API.
     */
    public interface GeminiCallback {
        /**
         * Called when the entire response has been successfully received and parsed.
         * @param finalResponse The complete, parsed response object.
         */
        void onComplete(GroundedResponse finalResponse);

        /**
         * Called if an error occurs during the API call or response parsing.
         * @param e The exception that occurred.
         */
        void onFailure(Exception e);
    }

    public GeminiService(String apiKey) {
        this.geminiApiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS) // Standard timeout is sufficient
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Generates a grounded response from a user query.
     *
     * @param userQuery The input text from the user.
     * @param callback  The callback to handle the final result or an error.
     */
    public void generateGroundedResponse(String userQuery, GeminiCallback callback) {
        HttpUrl url = HttpUrl.parse(BASE_URL + ":" + API_METHOD)
                .newBuilder()
                .addQueryParameter("key", geminiApiKey)
                .build();

        String requestJson = buildRequestJson(userQuery);
// Corrected order
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestJson);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Execute the call asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API call failed", e);
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        String errorBody = responseBody != null ? responseBody.string() : "Unknown error";
                        Log.e(TAG, "API call unsuccessful: " + response.code() + " " + errorBody);
                        callback.onFailure(new IOException("API call failed with code: " + response.code()));
                        return;
                    }

                    // Handle the complete, non-streamed response
                    handleFinalResponse(responseBody.string(), callback);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing API response", e);
                    callback.onFailure(e);
                }
            }
        });
    }

    /**
     * Handles the complete JSON response from the API.
     *
     * @param responseBodyString The full response body as a string.
     * @param callback           The callback to notify of the result.
     */
    private void handleFinalResponse(String responseBodyString, GeminiCallback callback) {
        try {
            // Extract the generated text from the main response object
            String generatedJsonText = extractTextFromResponse(responseBodyString);
            if (generatedJsonText == null) {
                callback.onFailure(new Exception("Could not extract generated text from API response."));
                return;
            }

            // The model output might include markdown backticks for the JSON block, remove them.
            String cleanJson = generatedJsonText.replace("```json", "").replace("```", "").trim();

            // Parse the cleaned JSON string into our final response object
            GroundedResponse finalResponse = gson.fromJson(cleanJson, GroundedResponse.class);
            callback.onComplete(finalResponse);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing the final JSON response.", e);
            callback.onFailure(e);
        }
    }

    /**
     * Safely extracts the generated text from the full API response JSON.
     * @param responseBodyString The complete JSON response string from the API.
     * @return The text content, or null if not found.
     */
    private String extractTextFromResponse(String responseBodyString) {
        try {
// Correct: Creating a new instance of JsonParser first
            JsonObject responseJson = new JsonParser().parse(responseBodyString).getAsJsonObject();
            JsonArray candidates = responseJson.getAsJsonArray("candidates");

            // FIX: Replaced !candidates.isEmpty() with candidates.size() > 0 for broader compatibility
            if (candidates != null && candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject content = candidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");

                // FIX: Replaced !parts.isEmpty() with parts.size() > 0
                if (parts != null && parts.size() > 0) {
                    JsonObject part = parts.get(0).getAsJsonObject();
                    // Ensure the 'text' field exists before trying to access it
                    if (part.has("text")) {
                        return part.get("text").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating response JSON to find text", e);
        }
        return null;
    }

    /**
     * Builds the JSON payload for the Gemini API request.
     *
     * @param userInput The user's query to be inserted into the prompt.
     * @return A JSON string representing the request body.
     */
    private String buildRequestJson(String userInput) {
        // --- System Instruction ---
        JsonObject systemInstructionText = new JsonObject();
        systemInstructionText.addProperty("text", "You are an expert historian who is capable of finding details about a historical figure from their name and the date of their death. You must use grounding search tool to verify the information. Always return the response with this JSON format:\n\n{\n\"name\" : \"Full name of the historical figure\",\n\"birth\" : \"Birth date if there's any information\",\n\"details\": \"Detailed 3-paragraph biography about the person (clean format with source annotation)\",\n\"sources\": [{\"source number\": \"Links of the information sources\"}]\n}");
        JsonArray systemInstructionParts = new JsonArray();
        systemInstructionParts.add(systemInstructionText);
        JsonObject systemInstruction = new JsonObject();
        systemInstruction.add("parts", systemInstructionParts);

        // --- Few-Shot Example (User) ---
        JsonObject userShotText = new JsonObject();
        userShotText.addProperty("text", "Who was John III, pope of the Catholic Church that was deceased in 574");
        JsonArray userShotParts = new JsonArray();
        userShotParts.add(userShotText);
        JsonObject userShot = new JsonObject();
        userShot.addProperty("role", "user");
        userShot.add("parts", userShotParts);

        // --- Few-Shot Example (Model) ---
        JsonObject modelShotText = new JsonObject();
        modelShotText.addProperty("text", "```json\n{\n\"name\": \"John III\",\n\"birth\": \"Around 530 AD [3]\",\n\"details\": \"John III, born Catelinus in Rome, was the Pope of the Catholic Church from July 17, 561, to his death on July 13, 574 [1, 3, 5]. Born to a distinguished family, his father, Anastasius, held the title of illustris [1, 3]. His papacy occurred during the Lombard invasion of Italy, a period of significant upheaval, resulting in the destruction of many records from his reign [1, 2, 3].\\n\\nDespite the challenges of his time, John III is remembered as a magnanimous pontiff who was dedicated to the welfare of the people [1]. In one notable act, he intervened on behalf of two bishops, Salonius of Embrun and Sagittarius of Gap, who had been condemned at a synod in Lyons. King Guntram of Burgundy believed they were unjustly condemned and appealed to John, who decided they should be restored to their sees [1, 5].\\n\\nDuring the Lombard invasion, John III sought assistance from Narses, the governor of Naples, to defend Rome [2, 3]. He even retreated to the catacombs of Praetextatus for several months, where he continued to perform ordinations [1]. After Narses' death, he returned to the Lateran Palace and, with a newfound appreciation for the catacombs, ordered their repair and ensured they received the necessities for Mass [1]. He was buried in St. Peter's [1].\",\n\"sources\": [\n  {\n    \"1\": \"[https://example.com/source1](https://example.com/source1)\"\n  },\n  {\n    \"2\": \"[https://example.com/source2](https://example.com/source2)\"\n  },\n  {\n    \"3\": \"[https://example.com/source3](https://example.com/source3)\"\n  },\n  {\n    \"4\": \"[https://example.com/source4](https://example.com/source4)\"\n  },\n  {\n    \"5\": \"[https://example.com/source5](https://example.com/source5)\"\n  }\n]\n}```");
        JsonArray modelShotParts = new JsonArray();
        modelShotParts.add(modelShotText);
        JsonObject modelShot = new JsonObject();
        modelShot.addProperty("role", "model");
        modelShot.add("parts", modelShotParts);

        // --- Actual User Query ---
        JsonObject userMessageText = new JsonObject();
        userMessageText.addProperty("text", userInput);
        JsonArray userMessageParts = new JsonArray();
        userMessageParts.add(userMessageText);
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.add("parts", userMessageParts);

        // --- Contents Array ---
        JsonArray contents = new JsonArray();
        contents.add(userShot);
        contents.add(modelShot);
        contents.add(userMessage);

        // --- Tools ---
        JsonObject googleSearchTool = new JsonObject();
        googleSearchTool.add("google_search", new JsonObject()); // Empty object enables the tool
        JsonArray tools = new JsonArray();
        tools.add(googleSearchTool);

        // --- Generation Config ---
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.2);
//        generationConfig.addProperty("responseMimeType", "application/json");

        // --- Main Request Body ---
        JsonObject requestBody = new JsonObject();
        requestBody.add("systemInstruction", systemInstruction);
        requestBody.add("contents", contents);
        requestBody.add("tools", tools);
        requestBody.add("generationConfig", generationConfig);

        return gson.toJson(requestBody);
    }

    /**
     * Data class to hold the final parsed JSON response from the Gemini API.
     */
    public static class GroundedResponse {
        @SerializedName("name")
        public String name;

        @SerializedName("birth")
        public String birth;

        @SerializedName("details")
        public String details;

        // e.g., [{"1": "url1"}, {"2": "url2"}]
        @SerializedName("sources")
        public List<Map<String, String>> sources;

        @NonNull
        @Override
        public String toString() {
            return "GroundedResponse{" +
                    "name='" + name + '\'' +
                    ", birth='" + birth + '\'' +
                    ", details='" + details + '\'' +
                    ", sources=" + sources +
                    '}';
        }
    }
}