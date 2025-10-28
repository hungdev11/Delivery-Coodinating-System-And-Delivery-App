package com.ds.communication_service.common.dto;

import java.time.LocalDateTime;

import com.ds.communication_service.common.enums.ContentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class MessageResponse {
    private String id;
    private String senderId;
    private String content;
    private LocalDateTime sentAt;
    private ContentType type;
}
