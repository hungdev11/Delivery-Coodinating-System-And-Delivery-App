package com.ds.deliveryapp.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.deliveryapp.clients.ParcelClient;
import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.res.BaseResponse;
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
    private final MutableLiveData<Boolean> statusChangeSuccess = new MutableLiveData<>();


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

    public LiveData<Boolean> getStatusChangeSuccess() {
        return statusChangeSuccess;
    }

    public void trackParcel(String code) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        //parcelResult.setValue(null);
        shipperInfoResult.setValue(null);

        // --- Logic gọi API thật ---

        // 1. Gọi API lấy thông tin đơn hàng
        parcelClient.getParcelByCode(code)
                .enqueue(new Callback<BaseResponse<Parcel>>() {
                    @Override
                    public void onResponse(@NonNull Call<BaseResponse<Parcel>> call, @NonNull Response<BaseResponse<Parcel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<Parcel> baseResponse = response.body();
                            if (baseResponse.getResult() != null) {
                                Parcel parcel = baseResponse.getResult();
                                parcelResult.setValue(parcel);
                                Log.i("PPPP", parcel.getCode());
                                // 2. Nếu thành công, gọi API lấy thông tin tài xế
                                fetchShipperInfo(parcel.getId());
                            } else {
                                isLoading.setValue(false);
                                String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không tìm thấy đơn hàng";
                                errorMessage.setValue(errorMsg);
                            }
                        } else {
                            isLoading.setValue(false);
                            errorMessage.setValue("Không tìm thấy đơn hàng?: " + response.code());
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<BaseResponse<Parcel>> call, @NonNull Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Lỗi mạng (Parcel): " + t.getMessage());
                    }
                });
    }

    private void fetchShipperInfo(String parcelId) {
        // 2. Gọi API lấy thông tin tài xế
        sessionClient.getLastestShipperInfoForParcel(parcelId)
                .enqueue(new Callback<BaseResponse<ShipperInfo>>() {
                    @Override
                    public void onResponse(@NonNull Call<BaseResponse<ShipperInfo>> call, @NonNull Response<BaseResponse<ShipperInfo>> response) {
                        isLoading.setValue(false); // (Hoàn thành cả 2 API)
                        Log.i("PASD", parcelId);
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<ShipperInfo> baseResponse = response.body();
                            if (baseResponse.getResult() != null) {
                                // Có thông tin tài xế
                                Log.i("PASD", "Have shipper");
                                shipperInfoResult.setValue(baseResponse.getResult());
                            } else {
                                // Không có tài xế (result null) -> bình thường
                                Log.i("PASD", "No shipper");
                                shipperInfoResult.setValue(null);
                            }
                        } else {
                            // Không có tài xế (response 404 hoặc rỗng) -> bình thường
                            Log.i("PASD", "No shipper (response error)");
                            shipperInfoResult.setValue(null);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<BaseResponse<ShipperInfo>> call, @NonNull Throwable t) {
                        isLoading.setValue(false);
                        // Lỗi, nhưng vẫn hiển thị thông tin đơn hàng
                        shipperInfoResult.setValue(null);
                        errorMessage.setValue("Lỗi khi lấy thông tin tài xế.");
                    }
                });
    }

    public void changeParcelStatus(String parcelId, String event) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        parcelClient.changeParcelStatus(parcelId, event)
                .enqueue(new Callback<BaseResponse<Parcel>>() {
                    @Override
                    public void onResponse(@NonNull Call<BaseResponse<Parcel>> call, @NonNull Response<BaseResponse<Parcel>> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<Parcel> baseResponse = response.body();
                            if (baseResponse.getResult() != null) {
                                parcelResult.setValue(baseResponse.getResult());
                                statusChangeSuccess.setValue(true);
                            } else {
                                String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể cập nhật trạng thái";
                                errorMessage.setValue(errorMsg);
                                statusChangeSuccess.setValue(false);
                            }
                        } else {
                            errorMessage.setValue("Không thể cập nhật trạng thái (" + response.code() + ")");
                            statusChangeSuccess.setValue(false);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<BaseResponse<Parcel>> call, @NonNull Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Lỗi mạng (Đổi trạng thái): " + t.getMessage());
                        statusChangeSuccess.setValue(false);
                    }
                });
    }

}
