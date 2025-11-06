package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.interfaces.ISessionServiceClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SessionController {

    private final ISessionServiceClient sessionServiceClient;

    @PostMapping("/drivers/{deliveryManId}/accept-parcel")
    public ResponseEntity<?> acceptParcelToSession(
            @PathVariable String deliveryManId,
            @Valid @RequestBody Object scanParcelRequest
    ) {
        return sessionServiceClient.acceptParcelToSession(deliveryManId, scanParcelRequest);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSessionById(@PathVariable UUID sessionId) {
        return sessionServiceClient.getSessionById(sessionId);
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<?> completeSession(@PathVariable UUID sessionId) {
        return sessionServiceClient.completeSession(sessionId);
    }

    @PostMapping("/{sessionId}/fail")
    public ResponseEntity<?> failSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody Object sessionFailRequest
    ) {
        return sessionServiceClient.failSession(sessionId, sessionFailRequest);
    }

    @PostMapping
    public ResponseEntity<?> createSessionBatch(@Valid @RequestBody Object createSessionRequest) {
        return sessionServiceClient.createSessionBatch(createSessionRequest);
    }
}
