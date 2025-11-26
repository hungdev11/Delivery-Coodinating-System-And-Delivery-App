package com.ds.deliveryapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.ParcelClient;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.Parcel;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Sử dụng AndroidViewModel để có thể truy cập Application Context
 */
public class ParcelListViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Parcel>> parcelList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private ParcelClient parcelClient;
    private AuthManager mAuthManager;

    public ParcelListViewModel(@NonNull Application application) {
        super(application);

        // Khởi tạo AuthManager để lấy UserID
        mAuthManager = new AuthManager(application);

        // Khởi tạo Retrofit Client MỘT LẦn dùng application context)
        Retrofit retrofit = RetrofitClient.getRetrofitInstance(application);
        parcelClient = retrofit.create(ParcelClient.class);
    }

    public LiveData<List<Parcel>> getParcelList() {
        return parcelList;
    }
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void fetchMyParcels() {
        isLoading.setValue(true);

        // 1. Lấy customerId từ AuthManager
        String customerId = mAuthManager.getUserId();

        if (customerId == null || customerId.isEmpty()) {
            errorMessage.setValue("Lỗi: Không tìm thấy ID người dùng. Vui lòng đăng nhập lại.");
            isLoading.setValue(false);
            return;
        }

        // 2. Gọi API với thông tin đã điền (trang 0, 20 mục)
        parcelClient.getParcelReceive(customerId, 0, 20)
                .enqueue(new Callback<BaseResponse<PageResponse<Parcel>>>() {
                    @Override
                    public void onResponse(@NonNull Call<BaseResponse<PageResponse<Parcel>>> call, @NonNull Response<BaseResponse<PageResponse<Parcel>>> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<PageResponse<Parcel>> baseResponse = response.body();
                            if (baseResponse.getResult() != null) {
                                // 3. Cập nhật LiveData với kết quả
                                parcelList.setValue(baseResponse.getResult().content());
                            } else {
                                String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể tải danh sách";
                                errorMessage.setValue(errorMsg);
                            }
                        } else {
                            errorMessage.setValue("Lỗi khi tải danh sách: " + response.code());
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<BaseResponse<PageResponse<Parcel>>> call, @NonNull Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Lỗi mạng: " + t.getMessage());
                    }
                });
    }

    public void fetchMySentParcels() {
        isLoading.setValue(true);

        // 1. Lấy customerId từ AuthManager
        String customerId = mAuthManager.getUserId();

        if (customerId == null || customerId.isEmpty()) {
            errorMessage.setValue("Lỗi: Không tìm thấy ID người dùng. Vui lòng đăng nhập lại.");
            isLoading.setValue(false);
            return;
        }

        // 2. Gọi API với thông tin đã điền (trang 0, 20 mục)
        parcelClient.getParcelSent(customerId, 0, 20)
                .enqueue(new Callback<BaseResponse<PageResponse<Parcel>>>() {
                    @Override
                    public void onResponse(@NonNull Call<BaseResponse<PageResponse<Parcel>>> call, @NonNull Response<BaseResponse<PageResponse<Parcel>>> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<PageResponse<Parcel>> baseResponse = response.body();
                            if (baseResponse.getResult() != null) {
                                // 3. Cập nhật LiveData với kết quả
                                parcelList.setValue(baseResponse.getResult().content());
                            } else {
                                String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể tải danh sách";
                                errorMessage.setValue(errorMsg);
                            }
                        } else {
                            errorMessage.setValue("Lỗi khi tải danh sách: " + response.code());
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<BaseResponse<PageResponse<Parcel>>> call, @NonNull Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Lỗi mạng: " + t.getMessage());
                    }
                });
    }
}
