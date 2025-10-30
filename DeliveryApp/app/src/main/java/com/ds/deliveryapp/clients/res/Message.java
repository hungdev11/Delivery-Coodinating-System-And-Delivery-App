package com.ds.deliveryapp.clients.res;

import com.ds.deliveryapp.enums.ContentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class Message {
    private String id;
    private String senderId;
    private String content;
    private String sentAt;
    private ContentType type;
}
