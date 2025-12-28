package com.ds.communication_service.application.controller.v2;

import com.ds.communication_service.business.v1.services.TicketService;
import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.PagedData;
import com.ds.communication_service.common.dto.TicketResponse;
import com.ds.communication_service.common.dto.request.PagingRequestV2;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * V2 API Controller for Ticket Management
 * V2: Enhanced dynamic filtering with operations between each pair of conditions
 */
@RestController
@RequestMapping("/api/v2/tickets")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TicketControllerV2 {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<BaseResponse<PagedData<TicketResponse>>> getTickets(@Valid @RequestBody PagingRequestV2 request) {
        log.debug("[communication-service] [TicketControllerV2.getTickets] POST /api/v2/tickets - Get tickets with enhanced filtering (V2)");
        PagedData<TicketResponse> pagedData = ticketService.getTicketsV2(request);
        return ResponseEntity.ok(BaseResponse.success(pagedData));
    }
}
