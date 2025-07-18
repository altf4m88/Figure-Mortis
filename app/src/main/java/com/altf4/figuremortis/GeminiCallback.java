package com.altf4.figuremortis;

public interface GeminiCallback {
    void onSuccess(GeminiResponse response);
    void onFailure(Exception e);
}
