package com.ds.session.session_service.business.v1.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.ds.session.session_service.common.entities.dto.response.UploadResult;
import com.ds.session.session_service.common.interfaces.ICloudinaryService;

@Service
public class CloudinaryService implements ICloudinaryService{

    private static final int MAX_FILES = 6;
    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public List<UploadResult> uploadImages(
            List<MultipartFile> files,
            String folder
    ) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files");
        }

        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException("Max 6 images");
        }

        List<UploadResult> results = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                validateImage(file);

                Map<?, ?> upload = cloudinary.uploader().upload(
                        file.getBytes(),
                        Map.of(
                            "folder", folder,
                            "resource_type", "image"
                        )
                );

                results.add(
                    new UploadResult(
                        upload.get("secure_url").toString(),
                        upload.get("public_id").toString()
                    )
                );
            }
            return results;
        } catch (Exception e) {
            // rollback nếu cần (delete đã upload)
            for (UploadResult r : results) {
                delete(r.publicId());
            }
            throw new RuntimeException("Upload failed", e);
        }
    }

    @Override
    public UploadResult uploadImage(MultipartFile file, String folder) {
        validateImage(file);
        try {
            Map<?, ?> upload = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of(
                        "folder", folder,
                        "resource_type", "image"
                    )
            );
            return new UploadResult(
                    upload.get("secure_url").toString(),
                    upload.get("public_id").toString()
            );
        } catch (Exception e) {
            throw new RuntimeException("Upload failed for file: " + file.getOriginalFilename(), e);
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, Map.of());
        } catch (Exception ignored) {}
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File > 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image allowed");
        }
    }

}

