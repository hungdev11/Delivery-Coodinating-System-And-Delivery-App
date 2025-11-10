package com.ds.deliveryapp.configs;

import android.content.Context;

import com.ds.deliveryapp.auth.AuthInterceptor;
import com.ds.deliveryapp.auth.TokenAuthenticator;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit sessionRetrofit;
    private static Retrofit authRetrofit;

    private static Retrofit chatRetrofit;
    private static final String BASE_URL = "https://localserver.phuongy.works";
    private static final String CHAT_BASE_URL = BASE_URL + "/api/v1/";
    private static final String GATEWAY_BASE_URL = BASE_URL + "/api/v1/";

    public static Retrofit getRetrofitInstance(Context context) {
        if (sessionRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(context))
                    .authenticator(new TokenAuthenticator(context))
                    .build();

            // 2. Khởi tạo Retrofit
            sessionRetrofit = new Retrofit.Builder()
                    .baseUrl(GATEWAY_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return sessionRetrofit;
    }

    public static Retrofit getAuthRetrofitInstance() {
        if (authRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder().build();

            authRetrofit = new Retrofit.Builder()
                    .baseUrl(GATEWAY_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }

    public static Retrofit getChatRetrofitInstance() {
        if (chatRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder().build();

            chatRetrofit = new Retrofit.Builder()
                    .baseUrl(CHAT_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return chatRetrofit;
    }
}