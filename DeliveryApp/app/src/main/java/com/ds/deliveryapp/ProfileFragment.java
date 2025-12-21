package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ds.deliveryapp.auth.AuthManager;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Button btnLogout = view.findViewById(R.id.btnLogout);
        Button btnMessages = view.findViewById(R.id.btnMessages);
        Button btnManageSessions = view.findViewById(R.id.btnManageSessions);

        btnLogout.setOnClickListener(v -> handleLogout());
        btnMessages.setOnClickListener(v -> openMessagesActivity());
        btnManageSessions.setOnClickListener(v -> openSessionListActivity());

        return view;
    }

    private void openMessagesActivity() {
        if (getContext() == null) return;
        
        Intent intent = new Intent(getContext(), ConversationsListActivity.class);
        startActivity(intent);
    }

    private void openSessionListActivity() {
        if (getContext() == null) return;
        
        Intent intent = new Intent(getContext(), SessionListActivity.class);
        startActivity(intent);
    }

    private void handleLogout() {
        if (getContext() == null || getActivity() == null) return;

        AuthManager authManager = new AuthManager(getContext());

        authManager.clearAuthData();

        Toast.makeText(getContext(), "Đăng xuất thành công", Toast.LENGTH_LONG).show();

        //Sử dụng Intent Flags để xóa sạch stack Activity (đóng MainActivity và tất cả Fragments)
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        //Kết thúc Activity cha (MainActivity) sau khi chuyển hướng
        getActivity().finish();
    }
}
