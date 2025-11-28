package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.req.ConfirmParcelRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.model.Parcel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ParcelClient {
    @GET("parcels/{parcelId}")
    Call<BaseResponse<Parcel>> getParcelById(@Path("parcelId") String parcelId);

    @GET("parcels/code/{code}")
    Call<BaseResponse<Parcel>> getParcelByCode(@Path("code") String code);

    @GET("parcels/me")
    Call<BaseResponse<PageResponse<Parcel>>> getParcelSent(
            @Query("customerId") String customerId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("parcels/me/receive")
    Call<BaseResponse<PageResponse<Parcel>>> getParcelReceive(@Query("customerId") String customerId,
                                                @Query("page") int page,
                                                @Query("size") int size
    );

    @PUT("parcels/change-status/{parcelId}")
    Call<BaseResponse<Parcel>> changeParcelStatus(@Path("parcelId") String parcelId, @Query("event") String event);
    
    @POST("parcels/{parcelId}/confirm")
    Call<BaseResponse<Parcel>> confirmParcel(@Path("parcelId") String parcelId, @Body ConfirmParcelRequest request);
}
