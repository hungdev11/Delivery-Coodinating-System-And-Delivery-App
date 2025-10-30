package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.req.LoginRequest;
import com.ds.deliveryapp.clients.req.RefreshTokenRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.KeycloakUserInfoDto;
import com.ds.deliveryapp.clients.res.LoginResponse;
import com.ds.deliveryapp.clients.res.RefreshTokenResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthClient {
    @POST("auth/login")
    Call<BaseResponse<LoginResponse>> login (@Body LoginRequest request);
    @POST("auth/refresh-token")
    Call<BaseResponse<RefreshTokenResponse>> refreshToken (@Body RefreshTokenRequest request);

    @GET("auth/me")
    Call<BaseResponse<KeycloakUserInfoDto>> getUserInfo(
            @Header("Authorization") String authorization
    );
}
