package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.model.Parcel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ParcelClient {
    @GET("parcels/{parcelId}")
    Call<Parcel> getParcelById(@Path("parcelId") String parcelId);
}
