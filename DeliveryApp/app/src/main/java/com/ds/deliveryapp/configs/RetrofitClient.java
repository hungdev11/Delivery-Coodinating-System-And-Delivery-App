package com.ds.deliveryapp.configs;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit gatewayRetrofit;
    private static Retrofit sessionRetrofit;

    private static final String BASE_URL = "http://192.168.1.6";
    private static final String GATEWAY_BASE_URL = BASE_URL + ":21506/api/v1/"; // URL cơ sở cho ParcelClient
    private static final String SESSION_BASE_URL = BASE_URL + ":21505/api/v1/"; // URL cơ sở cho SessionClient

    public static Retrofit getRetrofitInstance() {
        if (gatewayRetrofit == null) {
            gatewayRetrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(GATEWAY_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return gatewayRetrofit;
    }

    public static Retrofit getSessionRetrofitInstance() {
        if (sessionRetrofit == null) {
            sessionRetrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(SESSION_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return sessionRetrofit;
    }
}