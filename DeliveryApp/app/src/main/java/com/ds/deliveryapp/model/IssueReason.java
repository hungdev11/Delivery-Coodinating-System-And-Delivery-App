package com.ds.deliveryapp.model;

import lombok.Getter;

@Getter
public class IssueReason {
    public final String display; // Tên hiển thị (ví dụ: "Không liên lạc được")
    public final String code;    // Mã sự kiện gửi lên server (ví dụ: "CUSTOMER_UNREACHABLE")

    public IssueReason(String display, String code) {
        this.display = display;
        this.code = code;
    }

    @Override
    public String toString() {
        return display;
    }
}
