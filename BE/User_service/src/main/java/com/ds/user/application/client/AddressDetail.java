package com.ds.user.application.client;

import lombok.Data;

@Data
public class AddressDetail {
    private String id;
    private String name;
    private String addressText;
    private Double lat;
    private Double lon;
}
