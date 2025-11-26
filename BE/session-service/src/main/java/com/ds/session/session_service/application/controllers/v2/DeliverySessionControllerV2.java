package com.ds.session.session_service.application.controllers.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.common.BaseResponse;
import com.ds.session.session_service.common.entities.dto.common.PagedData;
import com.ds.session.session_service.common.entities.dto.request.PagingRequestV2;
import com.ds.session.session_service.common.entities.dto.response.SessionResponse;
import com.ds.session.session_service.common.interfaces.ISessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * V2 API Controller for Delivery Sessions
 * V2: Enhanced dynamic filtering with operations between each pair of conditions
 */
@RestController
@RequestMapping("/api/v2/delivery-sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DeliverySessionControllerV2 {

    private final ISessionService sessionService;

    @PostMapping
    public ResponseEntity<BaseResponse<PagedData<SessionResponse>>> searchSessions(
        @Valid @RequestBody PagingRequestV2 request
    ) {
        log.debug("[session-service] [DeliverySessionControllerV2.searchSessions] POST /api/v2/delivery-sessions - Search delivery sessions with enhanced filtering (V2)");
        PagedData<SessionResponse> pagedData = sessionService.searchSessionsV2(request);
        return ResponseEntity.ok(BaseResponse.success(pagedData));
    }
}
