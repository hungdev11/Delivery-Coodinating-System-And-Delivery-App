package com.ds.session.session_service.common.interfaces;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ds.session.session_service.common.entities.dto.response.UploadResult;

public interface ICloudinaryService {
    List<UploadResult> uploadImages(List<MultipartFile> files,String folder);
    UploadResult uploadImage(MultipartFile file, String folder);
}
