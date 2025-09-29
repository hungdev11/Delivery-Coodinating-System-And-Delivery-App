package com.ds.deliveryapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Toast;
import com.ds.deliveryapp.model.Order;
import com.ds.deliveryapp.model.OrderMockData;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.List;

public class QrScanActivity extends AppCompatActivity {

    private List<Order> mockOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Không cần layout — ZXing tự bật camera
        mockOrders = OrderMockData.getMockOrders();

        // Bắt đầu quét QR
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Quét mã QR trên đơn hàng");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Hủy quét mã", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                handleScannedCode(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedCode(String scannedCode) {
        // Giả định mã QR chứa ID đơn hàng, ví dụ "ORD002"
        Order order = findOrderById(scannedCode);

        if (order != null) {
            // Chuyển sang trang chi tiết đơn hàng
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("id", order.getId());
            intent.putExtra("customerName", order.getCustomerName());
            intent.putExtra("address", order.getAddress());
            intent.putExtra("phone", order.getPhone());
            intent.putExtra("receiveTime", order.getReceiveTime());
            intent.putExtra("amount", order.getTotalAmount());
            intent.putExtra("status", order.getStatus());
            intent.putExtra("paymentMethod", "Tiền mặt");
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Không tìm thấy đơn hàng tương ứng!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private Order findOrderById(String id) {
        for (Order o : mockOrders) {
            if (o.getId().equalsIgnoreCase(id.trim())) {
                return o;
            }
        }
        return null;
    }
}
