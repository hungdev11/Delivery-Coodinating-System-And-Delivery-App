package com.ds.deliveryapp.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

public class CloudinaryService {
    private static final String TAG = "CloudinaryService";

    private static final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/df8ula2bv/image/upload";
    private static final String CLOUDINARY_UPLOAD_PRESET = "proof_med";

    private static CloudinaryService instance;
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
     * Upload danh sách ảnh
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
                String url = uploadSingleImageSync(context, uri);
                if (url != null) {
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
     * Hàm xử lý upload 1 ảnh (Chạy blocking - nên gọi trong worker thread)
     */
    private String uploadSingleImageSync(Context context, Uri uri) {
        File imageFile = null;
        try {
            imageFile = getFileFromUri(context, uri);
            if (imageFile == null) return null;

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageFile.getName(),
                            RequestBody.create(imageFile, MediaType.parse("image/*")))
                    .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url(CLOUDINARY_UPLOAD_URL)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    if (jsonResponse.has("secure_url")) {
                        return jsonResponse.get("secure_url").getAsString();
                    }
                } else {
                    Log.e(TAG, "Upload failed: " + response.code());
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
