package com.ds.deliveryapp.model;

import java.text.NumberFormat;
import java.util.Locale;

public class Order {

    private String id;
    private String customerName;
    private String address;
    private String phone;
    private String receiveTime;
    private int totalAmount;
    private String status;
    private String paymentMethod; // optional (cash, online)

    public Order(String id, String customerName, String address, String phone,
                 String receiveTime, int totalAmount, String status) {
        this(id, customerName, address, phone, receiveTime, totalAmount, status, "cash");
    }

    public Order(String id, String customerName, String address, String phone,
                 String receiveTime, int totalAmount, String status, String paymentMethod) {
        this.id = id;
        this.customerName = customerName;
        this.address = address;
        this.phone = phone;
        this.receiveTime = receiveTime;
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentMethod = paymentMethod;
    }

    // Getter
    public String getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getReceiveTime() { return receiveTime; }
    public int getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }

    // Setter
    public void setStatus(String status) { this.status = status; }

    public String getFormattedAmount() {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                .format(totalAmount) + "đ";
    }
}
