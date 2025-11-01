package com.ds.deliveryapp.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.deliveryapp.clients.ParcelClient;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.res.ShipperInfo;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.Parcel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ParcelTrackViewModel extends AndroidViewModel {

    private final MutableLiveData<Parcel> parcelResult = new MutableLiveData<>();
    private final MutableLiveData<ShipperInfo> shipperInfoResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // --- Khai báo API Clients ---
    private ParcelClient parcelClient;
    private SessionClient sessionClient;

    // --- THÊM HÀM KHỞI TẠO (CONSTRUCTOR) ---
    public ParcelTrackViewModel(@NonNull Application application) {
        super(application);

        // Khởi tạo Retrofit cho Parcel Service
        Retrofit parcelRetrofit = RetrofitClient.getRetrofitInstance(application);
        parcelClient = parcelRetrofit.create(ParcelClient.class);

        // Khởi tạo Retrofit cho Session/Assignment Service
        Retrofit sessionRetrofit = RetrofitClient.getRetrofitInstance(application);
        sessionClient = sessionRetrofit.create(SessionClient.class);
    }


    public LiveData<Parcel> getParcelResult() {
        return parcelResult;
    }
    public LiveData<ShipperInfo> getShipperInfoResult() {
        return shipperInfoResult;
    }
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void trackParcel(String code) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        //parcelResult.setValue(null);
        shipperInfoResult.setValue(null);

        // --- Logic gọi API thật ---

        // 1. Gọi API lấy thông tin đơn hàng
        parcelClient.getParcelByCode(code)
                .enqueue(new Callback<Parcel>() {
                    @Override
                    public void onResponse(@NonNull Call<Parcel> call, @NonNull Response<Parcel> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Parcel parcel = response.body();
                            parcelResult.setValue(parcel);
                            Log.i("PPPP", response.body().getCode());
                            // 2. Nếu thành công, gọi API lấy thông tin tài xế
                            fetchShipperInfo(parcel.getId());
                        } else {
                            isLoading.setValue(false);
                            errorMessage.setValue("Không tìm thấy đơn hàng?: " + response.code());
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Parcel> call, @NonNull Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Lỗi mạng (Parcel): " + t.getMessage());
                    }
                });
    }

    private void fetchShipperInfo(String parcelId) {
        // 2. Gọi API lấy thông tin tài xế
        sessionClient.getLastestShipperInfoForParcel(parcelId)
                .enqueue(new Callback<ShipperInfo>() {
                    @Override
                    public void onResponse(@NonNull Call<ShipperInfo> call, @NonNull Response<ShipperInfo> response) {
                        isLoading.setValue(false); // (Hoàn thành cả 2 API)
                        Log.i("PASD", parcelId);
                        if (response.isSuccessful() && response.body() != null) {
                            // Có thông tin tài xế
                            Log.i("PASD", "Have shipper");
                            shipperInfoResult.setValue(response.body());
                        } else {
                            // Không có tài xế (response 404 hoặc rỗng) -> bình thường
                            Log.i("PASD", "Hve shipper");
                            shipperInfoResult.setValue(null);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ShipperInfo> call, @NonNull Throwable t) {
                        isLoading.setValue(false);
                        // Lỗi, nhưng vẫn hiển thị thông tin đơn hàng
                        shipperInfoResult.setValue(null);
                        errorMessage.setValue("Lỗi khi lấy thông tin tài xế.");
                    }
                });
    }
}
