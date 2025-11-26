package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.req.RoutingRequestDto;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.RoutingResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RoutingApi {
    @POST("/api/v1/routing/demo-route")
    Call<BaseResponse<RoutingResponseDto>> getOptimalRoute(
            @Body RoutingRequestDto.RouteRequestDto request
    );
}
