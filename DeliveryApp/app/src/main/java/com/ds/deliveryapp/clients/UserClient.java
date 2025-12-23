package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.UserDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface UserClient {
    @GET("users/me")
    Call<BaseResponse<UserDto>> getCurrentUser(
            @Header("Authorization") String authorization
    );
}
