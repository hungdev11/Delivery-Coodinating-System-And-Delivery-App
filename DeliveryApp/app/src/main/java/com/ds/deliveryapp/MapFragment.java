package com.ds.deliveryapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.ds.deliveryapp.R;

public class MapFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        Button btnStart = view.findViewById(R.id.btnStartTrip);
//        Button btnScan = view.findViewById(R.id.btnScanQR);

        btnStart.setOnClickListener(v -> Toast.makeText(getContext(), "Bắt đầu chuyến đi", Toast.LENGTH_SHORT).show());
//        btnScan.setOnClickListener(v -> Toast.makeText(getContext(), "Mở máy quét QR", Toast.LENGTH_SHORT).show());

        return view;
    }
}
