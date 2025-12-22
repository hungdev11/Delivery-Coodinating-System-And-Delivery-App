package com.ds.session.session_service.application.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ds.session.session_service.common.entities.dto.response.UploadResult;
import com.ds.session.session_service.common.interfaces.ICloudinaryService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class CloudinaryController {

    private final ICloudinaryService cloudinaryService;


    @PostConstruct
    public void init() {
        System.out.println(">>> CloudinaryController LOADED");
    }

    @PostMapping("/ping")
    public String ping() {
        return "OK";
    }

    // API 1: Upload 1 ảnh
    // POST /api/v1/uploads/single?folder=user_avatars
    @PostMapping("/single")
    public ResponseEntity<UploadResult> uploadSingle(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "android_uploads") String folder
    ) {
        UploadResult result = cloudinaryService.uploadImage(file, folder);
        return ResponseEntity.ok(result);
    }

    // API 2: Upload nhiều ảnh (Batch)
    // POST /api/v1/uploads/batch?folder=user_photos
    @PostMapping("/batch")
    public ResponseEntity<List<UploadResult>> uploadBatch(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "android_uploads") String folder
    ) {
        List<UploadResult> results = cloudinaryService.uploadImages(files, folder);
        return ResponseEntity.ok(results);
    }
}
