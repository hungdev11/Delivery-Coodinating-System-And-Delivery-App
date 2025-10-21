package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.req.RouteInfo;
import com.ds.deliveryapp.model.DeliveryAssignment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SessionClient {
    @GET("assignments/today/{id}")
    Call<PageResponse<DeliveryAssignment>> getTasksToday(@Path("id") String driverId,
                                                 @Query("status") List<String> status,
                                                 @Query("page") int page,
                                                 @Query("size") int size);

    @GET("assignments/{id}")
    Call<PageResponse<DeliveryAssignment>> getTasks(
            @Path("id") String driverId,
            @Query("status") List<String> status,
            // Sửa kiểu dữ liệu từ LocalDate sang String
            @Query("createdAtStart") String createdAtStart,
            @Query("createdAtEnd") String createdAtEnd,
            @Query("completedAtStart") String completedAtStart,
            @Query("completedAtEnd") String completedAtEnd,
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("assignments/{parcelId}/accept")
    Call<Boolean> acceptTask(@Path("parcelId") String parcelId,
                             @Query("deliveryManId") String deliveryManId);

    @PUT("assignments/{parcelId}/complete")
    Call<DeliveryAssignment> completeTask(@Path("parcelId") String parcelId,
                              @Query("deliveryManId") String deliveryManId,
                             @Body RouteInfo routeInfo
    );

    @PUT("assignments/{parcelId}/fail")
    Call<DeliveryAssignment> failTask(@Path("parcelId") String parcelId,
                               @Query("deliveryManId") String deliveryManId,
                                @Query("flag") boolean flag,
                               @Query("reason") String reason,
                           @Body RouteInfo routeInfo
    );

}
