package com.altf4.figuremortis;

import java.util.List;
import java.util.Map;

public class GeminiResponse {
    private String name;
    private String birth;
    private String details;
    private List<Map<String, String>> sources;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBirth() { return birth; }
    public void setBirth(String birth) { this.birth = birth; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public List<Map<String, String>> getSources() { return sources; }
    public void setSources(List<Map<String, String>> sources) { this.sources = sources; }
}
