package com.ds.deliveryapp.customer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ds.deliveryapp.R;
import com.ds.deliveryapp.enums.ParcelStatus;
import com.ds.deliveryapp.model.Parcel;
import com.ds.deliveryapp.utils.FormaterUtil;

import java.time.format.DateTimeFormatter;

public class ParcelDetailFragment extends Fragment {

    private Parcel parcelData;
    private static final String PARCEL_DATA_KEY = "PARCEL_DATA";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static ParcelDetailFragment newInstance(Parcel parcel) {
        ParcelDetailFragment fragment = new ParcelDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(PARCEL_DATA_KEY, parcel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof com.ds.deliveryapp.customer.MainActivity)) {
            throw new RuntimeException("Host Activity must be customer.MainActivity");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("FRAG_DEBUG", "ParcelListFragment visible");
        if (getArguments() != null) {
            parcelData = (Parcel) getArguments().getSerializable(PARCEL_DATA_KEY);
        }

        if (parcelData == null) {
            Toast.makeText(getContext(), "Lỗi tải dữ liệu đơn hàng.", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.customer_fragment_parcel_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btn_detail_back);
        TextView tvCode = view.findViewById(R.id.tv_detail_code);
        TextView tvStatus = view.findViewById(R.id.tv_detail_status);
        TextView tvFrom = view.findViewById(R.id.tv_detail_from);
        TextView tvTo = view.findViewById(R.id.tv_detail_to);
        TextView tvWeight = view.findViewById(R.id.tv_detail_weight);
        TextView tvCreatedAt = view.findViewById(R.id.tv_detail_created_at);

        // Gán dữ liệu
        if (parcelData != null) {
            tvCode.setText(parcelData.getCode());
            tvStatus.setText(getStatusText(parcelData.getStatus()));
            tvStatus.setTextColor(getStatusColor(parcelData.getStatus()));
            tvFrom.setText(parcelData.getReceiveFrom());
            tvTo.setText(parcelData.getTargetDestination());
            tvWeight.setText("Cân nặng: " + parcelData.getWeight() + " kg");
            tvCreatedAt.setText("Ngày tạo: " + FormaterUtil.formatDateTime(parcelData.getCreatedAt()));
        }

        btnBack.setOnClickListener(v -> {
            Log.d("BACK_DEBUG", "Back button clicked in ParcelDetailFragment");
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

    }

    private String getStatusText(ParcelStatus status) {
        if (status == null) return "Không rõ";
        switch (status) {
            case IN_WAREHOUSE: return "Trong kho";
            case ON_ROUTE: return "Đang vận chuyển";
            case DELIVERED: return "Đã giao (Chờ xác nhận)";
            case SUCCEEDED: return "Thành công";
            case FAILED: return "Giao thất bại";
            case DELAYED: return "Bị hoãn";
            case DISPUTE: return "Khiếu nại";
            case LOST: return "Thất lạc";
            default: return status.name();
        }
    }

    private int getStatusColor(ParcelStatus status) {
        Context context = getContext();
        if (context == null) return 0;

        int colorId;
        switch (status) {
            case SUCCEEDED:
            case DELIVERED:
                colorId = R.color.status_success;
                break;
            case FAILED:
            case DISPUTE:
            case LOST:
                colorId = R.color.status_failed;
                break;
            case ON_ROUTE:
                colorId = R.color.status_on_route;
                break;
            case DELAYED:
                colorId = R.color.status_pending;
                break;
            case IN_WAREHOUSE:
            default:
                colorId = R.color.status_pending;
                break;
        }
        return ContextCompat.getColor(context, colorId);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LIFE_DEBUG", getClass().getSimpleName() + " resumed");
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.d("LIFE_DEBUG", getClass().getSimpleName() + " paused");
    }

}
