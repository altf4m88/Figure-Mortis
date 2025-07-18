package com.altf4.figuremortis.service;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Collections;
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
import okio.BufferedSource;

/**
 * A service to interact with the Google Gemini API using the 'streamGenerateContent' method.
 * This service is designed for "grounded" generation, where the model's response is
 * grounded by Google Search results. It uses a few-shot prompting technique to guide
 * the model's output format.
 */
public class GeminiService {

    private final String TAG = "GroundedGenService";

    // Replace with actual API Key (store securely, not hardcoded in production)
    private final String geminiApiKey;
    private final String MODEL_ID = "gemini-2.0-flash";
    private static final String GENERATE_CONTENT_API_METHOD = "streamGenerateContent";
    private final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_ID;

    private final OkHttpClient httpClient;
    private final Gson gson;

    /**
     * Callback interface for handling the streaming response from the Gemini API.
     */
    public interface GroundedGenerationCallback {
        /**
         * Called for each chunk of text received from the stream.
         * @param textChunk A piece of the generated response.
         */
        void onStreamUpdate(String textChunk);

        /**
         * Called when the entire response has been successfully streamed and aggregated.
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
                .readTimeout(120, TimeUnit.SECONDS) // Increased timeout for streaming
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Main method to generate a grounded response from a user query.
     *
     * @param userQuery The input text from the user.
     * @param callback  The callback to handle streaming updates and the final result.
     */
    public void generateGroundedResponse(String userQuery, GroundedGenerationCallback callback) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + ":" + GENERATE_CONTENT_API_METHOD).newBuilder();
        urlBuilder.addQueryParameter("key", geminiApiKey);

        try {
            // Construct the complex JSON request body using Gson's object model
            // This is safer and more readable than using String.format.
            String requestJson = buildRequestJson(userQuery);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestJson);

            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .post(requestBody)
                    .build();

            // Execute the call asynchronously
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API call failed", e);
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    // The response body must be handled carefully for streaming
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful() || responseBody == null) {
                            String errorBody = responseBody != null ? responseBody.string() : "Unknown error";
                            Log.e(TAG, "API call unsuccessful: " + response.code() + " " + errorBody);
                            callback.onFailure(new IOException("API call failed: " + response.code() + " " + response.message()));
                            return;
                        }

                        // Process the stream chunk by chunk
                        handleStreamingResponse(responseBody, callback);

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing streaming response", e);
                        callback.onFailure(e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to build request JSON", e);
            callback.onFailure(e);
        }
    }

    /**
     * Handles the incoming streaming response from the API.
     *
     * @param responseBody The response body containing the stream of data.
     * @param callback     The callback to notify of updates.
     * @throws IOException if there is an issue reading the response.
     */
    private void handleStreamingResponse(ResponseBody responseBody, GroundedGenerationCallback callback) throws IOException {
        BufferedSource source = responseBody.source();
        StringBuilder aggregatedJson = new StringBuilder();

        try {
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line == null || line.trim().isEmpty() || !line.trim().startsWith("{")) {
                    continue; // Skip empty lines or non-JSON data
                }

                // The stream sends a list of JSON objects. We remove the surrounding brackets/commas
                // to parse each object individually.
                String cleanedLine = line.replace("[", "").replace("]", "").trim();
                if (cleanedLine.endsWith(",")) {
                    cleanedLine = cleanedLine.substring(0, cleanedLine.length() - 1);
                }

                try {
                    JsonObject chunk = new JsonParser().parse(cleanedLine).getAsJsonObject();
                    // Extract the text part from the chunk and send it as a stream update
                    String textChunk = extractTextFromChunk(chunk);
                    if (textChunk != null) {
                        aggregatedJson.append(textChunk);
                        callback.onStreamUpdate(textChunk); // Notify listener of the new chunk
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not parse a line of the stream: " + line, e);
                }
            }

            // After the stream is complete, parse the aggregated JSON string into the final object
            String finalJsonString = aggregatedJson.toString();
            // The model output includes markdown-style backticks for the JSON block, remove them.
            finalJsonString = finalJsonString.replace("```json", "").replace("```", "").trim();

            GroundedResponse finalResponse = gson.fromJson(finalJsonString, GroundedResponse.class);
            callback.onComplete(finalResponse);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing final aggregated JSON.", e);
            callback.onFailure(e);
        }
    }

    /**
     * Safely extracts the generated text from a JSON chunk of the stream.
     * @param chunk The JSON object from the stream.
     * @return The text content, or null if not found.
     */
    private String extractTextFromChunk(JsonObject chunk) {
        if (chunk.has("candidates")) {
            JsonArray candidates = chunk.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts.size() > 0) {
                            JsonObject part = parts.get(0).getAsJsonObject();
                            if (part.has("text")) {
                                return part.get("text").getAsString();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }


    /**
     * Builds the JSON payload for the Gemini API request, matching the required structure.
     *
     * @param userInput The user's query to be inserted into the prompt.
     * @return A JSON string representing the request body.
     */
    private String buildRequestJson(String userInput) {
        // Create the main request object
        JsonObject requestBody = new JsonObject();

        // --- Contents Array ---
        JsonArray contents = new JsonArray();

        // 1. First User Message (Few-shot example prompt)
        JsonObject userShot = new JsonObject();
        userShot.addProperty("role", "user");
        JsonArray userShotParts = new JsonArray();
        JsonObject userShotText = new JsonObject();
        userShotText.addProperty("text", "Who was John III, pope of the Catholic Church that was deceased in 574");
        userShotParts.add(userShotText);
        userShot.add("parts", userShotParts);
        contents.add(userShot);

        // 2. Model Response (Few-shot example response)
        JsonObject modelShot = new JsonObject();
        modelShot.addProperty("role", "model");
        JsonArray modelShotParts = new JsonArray();
        JsonObject modelShotText = new JsonObject();
        // This string contains the example JSON structure the model should follow.
        modelShotText.addProperty("text", "```json\n{\n\"name\": \"John III\",\n\"birth\": \"Around 530 AD [3]\",\n\"details\": \"John III, born Catelinus in Rome, was the Pope of the Catholic Church from July 17, 561, to his death on July 13, 574 [1, 3, 5]. Born to a distinguished family, his father, Anastasius, held the title of illustris [1, 3]. His papacy occurred during the Lombard invasion of Italy, a period of significant upheaval, resulting in the destruction of many records from his reign [1, 2, 3].\\n\\nDespite the challenges of his time, John III is remembered as a magnanimous pontiff who was dedicated to the welfare of the people [1]. In one notable act, he intervened on behalf of two bishops, Salonius of Embrun and Sagittarius of Gap, who had been condemned at a synod in Lyons. King Guntram of Burgundy believed they were unjustly condemned and appealed to John, who decided they should be restored to their sees [1, 5].\\n\\nDuring the Lombard invasion, John III sought assistance from Narses, the governor of Naples, to defend Rome [2, 3]. He even retreated to the catacombs of Praetextatus for several months, where he continued to perform ordinations [1]. After Narses' death, John returned to the Lateran Palace and, with a newfound appreciation for the catacombs, ordered their repair and ensured they received the necessities for Mass [1]. He was buried in St. Peter's [1].\",\n\"sources\": [\n  {\n    \"1\": \"[https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQHUh4hhPPY9GxRqSjfywQ7CAMJzGsOFCoVa5yVtuhTDuVMzJJlaVT_CC1ZPm2JaOBUjyZHw4lq4lIGP-ggVPtqbYEbkKYzxr3pronmkNEUhK1R5rr-fGOb6JDe4e4oeVj9x7V4FQTQ=](https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQHUh4hhPPY9GxRqSjfywQ7CAMJzGsOFCoVa5yVtuhTDuVMzJJlaVT_CC1ZPm2JaOBUjyZHw4lq4lIGP-ggVPtqbYEbkKYzxr3pronmkNEUhK1R5rr-fGOb6JDe4e4oeVj9x7V4FQTQ=)\"\n  },\n  {\n    \"2\": \"[https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQFgUxBscrR8RAseo-U9lQju8rXHHdrzSzzyZUKVd_iDnmyneJQbNipxBNDGUAq5319Jr0sEKlg3cP5dpHAWFdYa-N4PDBbw_bjprmfDRHp2cg3P56zyR8xDxLlbDo2jH8_lsMfDx5n202YKKTxb](https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQFgUxBscrR8RAseo-U9lQju8rXHHdrzSzzyZUKVd_iDnmyneJQbNipxBNDGUAq5319Jr0sEKlg3cP5dpHAWFdYa-N4PDBbw_bjprmfDRHp2cg3P56zyR8xDxLlbDo2jH8_lsMfDx5n202YKKTxb)\"\n  },\n  {\n    \"3\": \"[https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQG88AGHrR0t24xzp76TNXZvA_wc5VX6w302XP3h1OYy53hSs2w7lh32p1qZvO_RxJSaMz4V2wZ8DsRpVucIk-eNC5Qu6rHQu0_HYWtklI_OhLz3mWZryCTQqnpPHEnLYE3eaee-S9Gx](https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQG88AGHrR0t24xzp76TNXZvA_wc5VX6w302XP3h1OYy53hSs2w7lh32p1qZvO_RxJSaMz4V2wZ8DsRpVucIk-eNC5Qu6rHQu0_HYWtklI_OhLz3mWZryCTQqnpPHEnLYE3eaee-S9Gx)\"\n  },\n  {\n    \"4\": \"[https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQHR9EGkGId1-_etJm2IIIFmW13qjIiMUq-fO1sIk_Wg0KSoTbRdouF8XgZY4PdrfA8CYckcxdbTkYX1aDtFSj-O0y9jbuqGQAvv_9F5yL70bSJqrD9tetsrv-0mgIu3zsebOZhiLEnr_xBPEsMxqg==](https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQHR9EGkGId1-_etJm2IIIFmW13qjIiMUq-fO1sIk_Wg0KSoTbRdouF8XgZY4PdrfA8CYckcxdbTkYX1aDtFSj-O0y9jbuqGQAvv_9F5yL70bSJqrD9tetsrv-0mgIu3zsebOZhiLEnr_xBPEsMxqg==)\"\n  },\n  {\n    \"5\": \"[https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQEg4yyBHYdk1QkhFw5bK0C1ngiQLjWLt0Qd6QjCg2NSQuBpnY9OZgora5ZCLzGeGp8a-ngh_-8V6VEuG5W66sdB-E2KNr_CByfVLR5zKEuLHET_3jJo3CSSrWq7maTSEMaLCd4sMPE=](https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQEg4yyBHYdk1QkhFw5bK0C1ngiQLjWLt0Qd6QjCg2NSQuBpnY9OZgora5ZCLzGeGp8a-ngh_-8V6VEuG5W66sdB-E2KNr_CByfVLR5zKEuLHET_3jJo3CSSrWq7maTSEMaLCd4sMPE=)\"\n  }\n]\n}```");
        modelShotParts.add(modelShotText);
        modelShot.add("parts", modelShotParts);
        contents.add(modelShot);

        // 3. Final User Message (The actual query)
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        JsonArray userMessageParts = new JsonArray();
        JsonObject userMessageText = new JsonObject();
        userMessageText.addProperty("text", userInput);
        userMessageParts.add(userMessageText);
        userMessage.add("parts", userMessageParts);
        contents.add(userMessage);

        requestBody.add("contents", contents);

        // --- Generation Config ---
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.2);
        generationConfig.addProperty("responseMimeType", "application/json");
        requestBody.add("generationConfig", generationConfig);

        // --- System Instruction ---
        JsonObject systemInstruction = new JsonObject();
        JsonArray systemInstructionParts = new JsonArray();
        JsonObject systemInstructionText = new JsonObject();
        systemInstructionText.addProperty("text", "You are an expert historian who is capable of finding details about a historical figure from their name and the date of their death. You must use grounding search tool to verify the information. Always return the response with this JSON format:\n\n{\n\"name\" : \"Full name of the historical figure\",\n\"birth\" : \"Birth date if there's any information\",\n\"details\": \"Detailed 3-paragraph biography about the person (clean format with source annotation)\",\n\"sources\": [{\"source number\": \"Links of the information sources\"}]\n}");
        systemInstructionParts.add(systemInstructionText);
        systemInstruction.add("parts", systemInstructionParts);
        requestBody.add("systemInstruction", systemInstruction);

        // --- Tools ---
        JsonArray tools = new JsonArray();
        JsonObject googleSearchTool = new JsonObject();
        googleSearchTool.add("googleSearch", new JsonObject()); // Empty object enables the tool
        tools.add(googleSearchTool);
        requestBody.add("tools", tools);

        return gson.toJson(requestBody);
    }

    /**
     * Data class to hold the final parsed JSON response from the Gemini API.
     * Uses Gson's @SerializedName for mapping JSON keys to Java fields.
     */
    public static class GroundedResponse {
        @SerializedName("name")
        public String name;

        @SerializedName("birth")
        public String birth;

        @SerializedName("details")
        public String details;

        // The sources are a list of objects, where each object is a single-entry map.
        // e.g., [{"1": "url1"}, {"2": "url2"}]
        @SerializedName("sources")
        public List<Map<String, String>> sources;

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
