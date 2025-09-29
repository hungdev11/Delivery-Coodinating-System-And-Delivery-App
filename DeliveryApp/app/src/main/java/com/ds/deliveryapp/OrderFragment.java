package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.OrdersAdapter;
import com.ds.deliveryapp.model.Order;
import com.ds.deliveryapp.model.OrderMockData;

import java.util.List;

public class OrderFragment extends Fragment {

    private RecyclerView rvOrders;
    private OrdersAdapter adapter;
    private List<Order> orders;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);
        Button btnScanOrder = view.findViewById(R.id.btnScanOrder);
        btnScanOrder.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QrScanActivity.class);
            startActivity(intent);
        });

        rvOrders = view.findViewById(R.id.recyclerOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));

        orders = OrderMockData.getMockOrders();

        adapter = new OrdersAdapter(orders, order -> {
            Intent intent = new Intent(getActivity(), OrderDetailActivity.class);

            // Gửi toàn bộ thông tin đơn hàng qua Intent
            intent.putExtra("id", order.getId());
            intent.putExtra("customerName", order.getCustomerName());
            intent.putExtra("address", order.getAddress());
            intent.putExtra("phone", order.getPhone());
            intent.putExtra("receiveTime", order.getReceiveTime());
            intent.putExtra("amount", order.getTotalAmount());
            intent.putExtra("status", order.getStatus());
            intent.putExtra("paymentMethod", order.getPaymentMethod());

            startActivity(intent);
        });


        rvOrders.setAdapter(adapter);
        return view;
    }
}
