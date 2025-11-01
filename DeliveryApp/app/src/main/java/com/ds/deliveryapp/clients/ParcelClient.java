package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.model.Parcel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ParcelClient {
    @GET("parcels/{parcelId}")
    Call<Parcel> getParcelById(@Path("parcelId") String parcelId);

    @GET("parcels/code/{code}")
    Call<Parcel> getParcelByCode(@Path("code") String code);

    @GET("parcels/me")
    Call<PageResponse<Parcel>> getParcelSent(
            @Query("customerId") String customerId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("parcels/me/receive")
    Call<PageResponse<Parcel>> getParcelReceive(@Query("customerId") String customerId,
                                                @Query("page") int page,
                                                @Query("size") int size
    );
}
