package com.ds.deliveryapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.ds.deliveryapp.model.Order;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvOrderId, tvCustomerName, tvAddress, tvPhone, tvReceiveTime, tvTotalAmount, tvStatus;
    private Button btnArrived;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        initViews();
        loadDataFromIntent();  // ✅ load dynamic
        updateUI();
        setupActions();
    }

    private void initViews() {
        tvCustomerName = findViewById(R.id.tvPassengerName);
        tvAddress = findViewById(R.id.tvFullAddress);
        tvTotalAmount = findViewById(R.id.tvPrice);
        tvStatus = findViewById(R.id.tvArriveText);
        tvPhone = findViewById(R.id.tvPaymentMethod);
        btnArrived = findViewById(R.id.btnArrived);
    }

    private void loadDataFromIntent() {
        String id = getIntent().getStringExtra("id");
        String customerName = getIntent().getStringExtra("customerName");
        String address = getIntent().getStringExtra("address");
        String phone = getIntent().getStringExtra("phone");
        String receiveTime = getIntent().getStringExtra("receiveTime");
        int totalAmount = getIntent().getIntExtra("amount", 0);
        String status = getIntent().getStringExtra("status");
        String paymentMethod = getIntent().getStringExtra("paymentMethod");

        currentOrder = new Order(id, customerName, address, phone, receiveTime, totalAmount, status, paymentMethod);
    }

    private void updateUI() {
        tvCustomerName.setText(currentOrder.getCustomerName());
        tvAddress.setText(currentOrder.getAddress());
        tvTotalAmount.setText(currentOrder.getFormattedAmount());
        tvStatus.setText(getStatusLabel(currentOrder.getStatus()));
        tvPhone.setText("SĐT: " + currentOrder.getPhone());
    }


    private String getStatusLabel(String status) {
        switch (status) {
            case "pending": return "Chờ nhận";
            case "delivering": return "Đang giao";
            case "delivered": return "Đã giao";
            default: return "Không xác định";
        }
    }

    private void setupActions() {
        btnArrived.setOnClickListener(v -> {
            if (currentOrder.getStatus().equals("pending")) {
                currentOrder.setStatus("delivering");
                Toast.makeText(this, "Đã nhận đơn #" + currentOrder.getId(), Toast.LENGTH_SHORT).show();
            } else if (currentOrder.getStatus().equals("delivering")) {
                currentOrder.setStatus("delivered");
                Toast.makeText(this, "Đã giao đơn #" + currentOrder.getId(), Toast.LENGTH_SHORT).show();
            }
            updateUI();
        });
    }
}
