package com.ds.session.session_service.application.controllers;


import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ds.session.session_service.business.v1.services.QRService;

@RestController
@RequestMapping("/api/v1/qr")
public class QRController {

    @Autowired
    private QRService qrService;

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generate(@RequestParam String data) {
        try {
            byte[] qrImage = qrService.generateQRBytes(data);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrImage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/decode")
    public ResponseEntity<String> decode(@RequestParam("file") MultipartFile file) {
        try {
            File temp = File.createTempFile("qr-", ".png");
            file.transferTo(temp);
            String result = qrService.decodeQR(temp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi giải mã QR");
        }
    }

}

