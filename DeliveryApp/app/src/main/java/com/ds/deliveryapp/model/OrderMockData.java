package com.ds.deliveryapp.model;

import java.util.ArrayList;
import java.util.List;

public class OrderMockData {

    public static List<Order> getMockOrders() {
        List<Order> list = new ArrayList<>();

        list.add(new Order("ORD001", "Nguyễn Văn A",
                "12 Nguyễn Huệ, Q1, TP.HCM", "0909123123",
                "09:00 - 11:00", 250000, "pending"));

        list.add(new Order("ORD002", "Trần Thị C",
                "45 Hai Bà Trưng, Q3, TP.HCM", "0911222333",
                "13:00 - 15:00", 420000, "delivering"));

        list.add(new Order("ORD003", "Phạm Văn D",
                "88 Nguyễn Thị Minh Khai, Q1, TP.HCM", "0933444555",
                "16:00 - 18:00", 180000, "delivered"));

        list.add(new Order("ORD004", "Lê Thị E",
                "21 Điện Biên Phủ, Q3, TP.HCM", "0988777666",
                "18:00 - 20:00", 310000, "pending"));

        list.add(new Order("ORD005", "Phan Văn F",
                "99 Lý Tự Trọng, Q1, TP.HCM", "0977665544",
                "10:00 - 12:00", 500000, "delivering"));

        return list;
    }
}

