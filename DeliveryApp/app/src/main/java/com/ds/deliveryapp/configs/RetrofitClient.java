package com.ds.deliveryapp.configs;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit gatewayRetrofit;
    private static final String BASE_URL = "http://192.168.1.6";
    private static final String GATEWAY_BASE_URL = BASE_URL + ":21500/api/v1/"; // URL cơ sở cho ParcelClient
    public static Retrofit getRetrofitInstance() {
        if (gatewayRetrofit == null) {
            gatewayRetrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(GATEWAY_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return gatewayRetrofit;
    }
}