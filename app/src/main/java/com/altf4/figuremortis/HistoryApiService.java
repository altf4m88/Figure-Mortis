package com.altf4.figuremortis;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface HistoryApiService {
    @GET("date/{month}/{day}")
    Call<HistoryResponse> getEvents(@Path("month") int month, @Path("day") int day);
}
