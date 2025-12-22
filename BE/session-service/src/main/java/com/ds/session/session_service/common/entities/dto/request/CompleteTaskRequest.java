package com.ds.session.session_service.common.entities.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTaskRequest {
    private RouteInfo routeInfo;
    private List<String> proofImageUrls;
}
