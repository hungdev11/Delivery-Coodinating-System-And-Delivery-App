package com.ds.deliveryapp.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ds.deliveryapp.ChatActivity;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.clients.res.ShipperInfo;
import com.ds.deliveryapp.enums.ParcelStatus;
import com.ds.deliveryapp.model.Parcel;
import com.ds.deliveryapp.viewmodel.ParcelTrackViewModel;

public class ParcelTrackFragment extends Fragment {

    // 1. Khai báo các biến View
    private LinearLayout layoutConfirmButtons;
    private EditText editTextTrackCode;
    private Button buttonTrack;
    private ProgressBar progressBarTrack;
    private LinearLayout layoutTrackResult;
    private TextView textResultCode, textResultStatus, textResultDestination;
    private LinearLayout layoutShipperInfo;
    private TextView textShipperName, textShipperPhone;
    private Button buttonCallShipper, buttonChatShipper, buttonReceived, buttonNotReceived;

    // 2. Khai báo ViewModel
    private ParcelTrackViewModel viewModel;
    private ShipperInfo currentShipper = null;
    private Parcel currentParcel = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 3. Inflate layout và trả về View
        View view = inflater.inflate(R.layout.customer_fragment_parcel_track, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 4. Ánh xạ (findViewById) toàn bộ các View
        editTextTrackCode = view.findViewById(R.id.edit_text_track_code);
        buttonTrack = view.findViewById(R.id.button_track);
        progressBarTrack = view.findViewById(R.id.progress_bar_track);
        layoutTrackResult = view.findViewById(R.id.layout_track_result);
        textResultCode = view.findViewById(R.id.text_result_code);
        textResultStatus = view.findViewById(R.id.text_result_status);
        textResultDestination = view.findViewById(R.id.text_result_destination);
        layoutShipperInfo = view.findViewById(R.id.layout_shipper_info);
        textShipperName = view.findViewById(R.id.text_shipper_name);
        textShipperPhone = view.findViewById(R.id.text_shipper_phone);
        buttonCallShipper = view.findViewById(R.id.button_call_shipper);
        buttonChatShipper = view.findViewById(R.id.button_chat_shipper);
        layoutConfirmButtons = view.findViewById(R.id.layout_confirm_buttons);
        buttonReceived = view.findViewById(R.id.button_confirm_received);
        buttonNotReceived = view.findViewById(R.id.button_not_received);


        // 5. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(ParcelTrackViewModel.class);

        // 6. Gán sự kiện cho các nút
        setupListeners();

        // 7. Lắng nghe kết quả từ ViewModel
        observeViewModel();
    }

    private void setupListeners() {
        // Gán sự kiện cho nút "Theo dõi"
        buttonTrack.setOnClickListener(v -> {
            String code = editTextTrackCode.getText().toString().trim().toUpperCase();
            if (code.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập mã vận đơn", Toast.LENGTH_SHORT).show();
            } else {
                // Gọi ViewModel để bắt đầu tra cứu
                viewModel.trackParcel(code);
            }
        });

        // Gán sự kiện cho nút "Gọi điện"
        buttonCallShipper.setOnClickListener(v -> {
            if (currentShipper.getPhone() != null && !currentShipper.getPhone().isEmpty()) {
                // Tạo Intent để gọi điện (ACTION_DIAL)
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + currentShipper.getPhone()));
                startActivity(callIntent);
            } else {
                Toast.makeText(getContext(), "Không tìm thấy số điện thoại", Toast.LENGTH_SHORT).show();
            }
        });

        // Gán sự kiện cho nút "Nhắn tin" (Chat)
        buttonChatShipper.setOnClickListener(v -> {
            Intent chatIntent = new Intent(requireContext(), ChatActivity.class);
            // 2. Đóng gói (put) dữ liệu được yêu cầu

            chatIntent.putExtra("RECIPIENT_ID", currentShipper.getId());
            chatIntent.putExtra("RECIPIENT_NAME", currentShipper.getName());
            // Dữ liệu MỚI cho thanh tiêu đề (theo yêu cầu)
            chatIntent.putExtra("PARCEL_CODE", currentParcel.getCode());
            chatIntent.putExtra("PARCEL_ID", currentParcel.getId());
            // 3. Khởi chạy ChatActivity
            startActivity(chatIntent);
            Toast.makeText(getContext(), "Mở màn hình Chat...", Toast.LENGTH_SHORT).show();
        });

        buttonReceived.setOnClickListener(v -> {
            if (currentParcel != null) {
                viewModel.changeParcelStatus(currentParcel.getId(), "CUSTOMER_RECEIVED");
            }
        });

        buttonNotReceived.setOnClickListener(v -> {
            if (currentParcel != null) {
                viewModel.changeParcelStatus(currentParcel.getId(), "CUSTOMER_CONFIRM_NOT_RECEIVED");
            }
        });

    }

    private void observeViewModel() {
        // Lắng nghe trạng thái loading
        viewModel.isLoading().observe(getViewLifecycleOwner(), this::handleLoading);

        // Lắng nghe kết quả (đơn hàng)
        viewModel.getParcelResult().observe(getViewLifecycleOwner(), this::handleParcelSuccess);

        // Lắng nghe kết quả (tài xế)
        viewModel.getShipperInfoResult().observe(getViewLifecycleOwner(), this::handleShipperInfo);

        // Lắng nghe lỗi
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                handleError(error);
            }
        });

        // Lắng kết quả cập nhật trạng thái
        viewModel.getStatusChangeSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
                // Tự động tải lại thông tin đơn hàng sau khi đổi trạng thái
                if (currentParcel != null) {
                    viewModel.trackParcel(currentParcel.getCode());
                }
            }
        });
    }

    // Xử lý trạng thái đang tải
    private void handleLoading(boolean isLoading) {
        progressBarTrack.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonTrack.setEnabled(!isLoading);
        // Ẩn kết quả cũ khi bắt đầu tìm kiếm mới
        if (isLoading) {
            layoutTrackResult.setVisibility(View.GONE);
            layoutShipperInfo.setVisibility(View.GONE);
        }
    }

    // Xử lý khi lấy được thông tin đơn hàng
    private void handleParcelSuccess(Parcel parcel) {
        if (parcel == null) {
            handleError("Không tìm thấy đơn hàng.");
            return;
        }

        ParcelStatus status = parcel.getStatus();

        if (ParcelStatus.DELIVERED.equals(status)) {
            layoutConfirmButtons.setVisibility(View.VISIBLE);
        } else {
            layoutConfirmButtons.setVisibility(View.GONE);
        }

        currentParcel = parcel;
        layoutTrackResult.setVisibility(View.VISIBLE);
        textResultCode.setText("Mã đơn: " + (parcel.getCode() != null ? parcel.getCode() : "N/A"));
        // Use the status variable that was already extracted, with null check
        textResultStatus.setText("Trạng thái: " + (status != null ? status.toString() : "N/A"));
        textResultDestination.setText("Đến: " + (parcel.getTargetDestination() != null ? parcel.getTargetDestination() : "N/A"));
    }

    // Xử lý khi lấy được thông tin tài xế
    private void handleShipperInfo(ShipperInfo shipper) {
        if (shipper != null) {
            currentShipper = shipper;
            layoutShipperInfo.setVisibility(View.VISIBLE);
            textShipperName.setText("Tên: " + shipper.getName());
            textShipperPhone.setText("SĐT: " + shipper.getPhone());
        } else {
            // Ẩn vùng thông tin tài xế nếu API không trả về
            currentShipper = null;
            layoutShipperInfo.setVisibility(View.GONE);
        }
    }

    // Xử lý lỗi
    private void handleError(String error) {
        layoutTrackResult.setVisibility(View.GONE);
        layoutShipperInfo.setVisibility(View.GONE);
        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
    }
}
