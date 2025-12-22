package com.ds.deliveryapp.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class CloudinaryService {
    private static final String TAG = "CloudinaryService";

    private static final String CLOUDINARY_BASE_URL = "https://api.cloudinary.com/v1_1/df8ula2bv";
    private static final String CLOUDINARY_IMAGE_UPLOAD_URL = CLOUDINARY_BASE_URL + "/image/upload";
    private static final String CLOUDINARY_VIDEO_UPLOAD_URL = CLOUDINARY_BASE_URL + "/video/upload";
    private static final String CLOUDINARY_UPLOAD_PRESET = "proof_med";

    private static CloudinaryService instance;
    private static final String PREFS_NAME = "cloudinary_uploads";
    private static final String PREF_HASH_PREFIX = "hash_";
    
    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private CloudinaryService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        // Dùng ThreadPool để upload song song tối đa 4 ảnh cùng lúc -> Nhanh hơn tuần tự
        this.executorService = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized CloudinaryService getInstance() {
        if (instance == null) {
            instance = new CloudinaryService();
        }
        return instance;
    }

    public interface OnBatchUploadCallback {
        void onComplete(List<String> successfulUrls);
        void onError(String message);
    }

    /**
     * Upload danh sách ảnh/video
     */
    public void uploadImages(Context context, List<Uri> uris, OnBatchUploadCallback callback) {
        if (uris == null || uris.isEmpty()) {
            callback.onComplete(new ArrayList<>());
            return;
        }

        List<String> uploadedUrls = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        // Dùng CountDownLatch hoặc AtomicInteger để đếm số lượng ảnh đã xử lý
        java.util.concurrent.atomic.AtomicInteger processedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        int totalImages = uris.size();

        for (Uri uri : uris) {
            executorService.execute(() -> {
                String url = null;
                
                // Check if file hash already exists in cache
                String fileHash = calculateFileHash(context, uri);
                if (fileHash != null) {
                    url = getCachedUrl(context, fileHash);
                    if (url != null) {
                        Log.d(TAG, "Reusing cached URL for hash: " + fileHash.substring(0, 8) + "...");
                        uploadedUrls.add(url);
                        if (processedCount.incrementAndGet() == totalImages) {
                            mainHandler.post(() -> {
                                if (!uploadedUrls.isEmpty() || errors.isEmpty()) {
                                    callback.onComplete(new ArrayList<>(uploadedUrls));
                                } else {
                                    callback.onError("Không thể upload ảnh nào. Vui lòng thử lại.");
                                }
                            });
                        }
                        return; // Skip upload, use cached URL
                    }
                }
                
                // Detect MIME type to call appropriate function
                String mimeType = context.getContentResolver().getType(uri);
                Log.d(TAG, "Processing media - MIME type: " + mimeType + ", URI: " + uri);
                if (mimeType != null && mimeType.startsWith("video/")) {
                    Log.d(TAG, "Detected video, uploading to video endpoint...");
                    url = uploadSingleVideoSync(context, uri, fileHash);
                    Log.d(TAG, "Video upload result: " + (url != null ? "SUCCESS - " + url : "FAILED"));
                } else {
                    Log.d(TAG, "Detected image, uploading to image endpoint...");
                    url = uploadSingleImageSync(context, uri, fileHash);
                    Log.d(TAG, "Image upload result: " + (url != null ? "SUCCESS - " + url : "FAILED"));
                }
                
                if (url != null) {
                    // Cache the URL with hash
                    if (fileHash != null) {
                        cacheUrl(context, fileHash, url);
                    }
                    uploadedUrls.add(url);
                } else {
                    errors.add("Failed to upload: " + uri.toString());
                }

                // Kiểm tra xem đã xử lý xong hết chưa
                if (processedCount.incrementAndGet() == totalImages) {
                    mainHandler.post(() -> {
                        if (!uploadedUrls.isEmpty() || errors.isEmpty()) {
                            // Trả về danh sách URL thành công (kể cả khi có 1 vài ảnh lỗi, vẫn trả về các ảnh ok)
                            callback.onComplete(new ArrayList<>(uploadedUrls));
                        } else {
                            callback.onError("Không thể upload ảnh nào. Vui lòng thử lại.");
                        }
                    });
                }
            });
        }
    }

    /**
     * Tính hash MD5 của file để kiểm tra trùng lặp
     */
    private String calculateFileHash(Context context, Uri uri) {
        try {
            File file = getFileFromUri(context, uri);
            if (file == null || !file.exists()) return null;
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, "Error calculating file hash", e);
            return null;
        }
    }
    
    /**
     * Lấy URL đã cache từ hash
     */
    private String getCachedUrl(Context context, String hash) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_HASH_PREFIX + hash, null);
    }
    
    /**
     * Cache URL với hash
     */
    private void cacheUrl(Context context, String hash, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_HASH_PREFIX + hash, url).apply();
    }
    
    /**
     * Hàm xử lý upload 1 ảnh (Chạy blocking - nên gọi trong worker thread)
     */
    private String uploadSingleImageSync(Context context, Uri uri, String fileHash) {
        File imageFile = null;
        try {
            imageFile = getFileFromUri(context, uri);
            if (imageFile == null) return null;

            MediaType mediaType = MediaType.parse("image/*");
            RequestBody fileRequestBody = RequestBody.create(imageFile, mediaType);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageFile.getName(), fileRequestBody)
                    .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url(CLOUDINARY_IMAGE_UPLOAD_URL)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    if (jsonResponse.has("secure_url")) {
                        String imageUrl = jsonResponse.get("secure_url").getAsString();
                        Log.d(TAG, "Image uploaded successfully: " + imageUrl);
                        return imageUrl;
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e(TAG, "Image upload failed: " + response.code() + ", body: " + errorBody);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception uploading image", e);
        } finally {
            // Dọn dẹp file tạm
            if (imageFile != null && imageFile.exists()) {
                try {
                    boolean deleted = imageFile.delete();
                    if (!deleted) Log.w(TAG, "Could not delete temp file");
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Hàm xử lý upload 1 video (Chạy blocking - nên gọi trong worker thread)
     * Sử dụng custom RequestBody để tránh lỗi content-length mismatch
     */
    private String uploadSingleVideoSync(Context context, Uri uri, String fileHash) {
        File videoFile = null;
        try {
            videoFile = getFileFromUri(context, uri);
            if (videoFile == null) return null;

            // Create final reference for use in inner class
            final File finalVideoFile = videoFile;
            MediaType mediaType = MediaType.parse("video/*");
            
            // For videos, use a custom RequestBody that streams the file
            // to avoid content-length calculation issues
            RequestBody fileRequestBody = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return mediaType;
                }

                @Override
                public long contentLength() throws IOException {
                    return finalVideoFile.length();
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(finalVideoFile);
                         Source source = Okio.source(fis)) {
                        sink.writeAll(source);
                    }
                }
            };

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", videoFile.getName(), fileRequestBody)
                    .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url(CLOUDINARY_VIDEO_UPLOAD_URL)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    if (jsonResponse.has("secure_url")) {
                        String videoUrl = jsonResponse.get("secure_url").getAsString();
                        Log.d(TAG, "Video uploaded successfully: " + videoUrl);
                        return videoUrl;
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e(TAG, "Video upload failed: " + response.code() + ", body: " + errorBody);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception uploading video", e);
        } finally {
            // Dọn dẹp file tạm
            if (videoFile != null && videoFile.exists()) {
                try {
                    boolean deleted = videoFile.delete();
                    if (!deleted) Log.w(TAG, "Could not delete temp file");
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Helper chuyển Uri -> File
     */
    private File getFileFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            // Tạo file tạm trong cache
            File tempFile = new File(context.getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Error converting Uri to File", e);
            return null;
        }
    }
}
