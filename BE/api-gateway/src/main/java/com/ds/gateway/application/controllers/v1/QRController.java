package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.interfaces.ISessionServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
@Slf4j
public class QRController {

    private final ISessionServiceClient sessionServiceClient;

    @GetMapping("/generate")
    public ResponseEntity<?> generateQR(@RequestParam String data) {
        return sessionServiceClient.generateQR(data);
    }
}
