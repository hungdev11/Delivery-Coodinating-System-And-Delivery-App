package com.ds.deliveryapp.clients;
import com.ds.deliveryapp.clients.res.UserInfo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface UserClient {
    @GET("/api/v1/users/{id}")
    Call<UserInfo> getUserInfoById(@Path("id") String id);
}
