package com.ds.deliveryapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.location.Location;
import android.location.LocationManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.ds.deliveryapp.clients.SessionClient;
import com.ds.deliveryapp.clients.req.CompleteTaskRequest;
import com.ds.deliveryapp.clients.req.RouteInfo;
import com.ds.deliveryapp.clients.req.TaskFailRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.model.DeliveryAssignment;
import com.ds.deliveryapp.service.CloudinaryService;
import com.ds.deliveryapp.utils.SessionManager;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProofActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT_IMAGES = "EXTRA_RESULT_IMAGES";
    public static final String EXTRA_ASSIGNMENT_ID = "EXTRA_ASSIGNMENT_ID";
    public static final String EXTRA_PARCEL_ID = "EXTRA_PARCEL_ID";
    public static final String EXTRA_DRIVER_ID = "EXTRA_DRIVER_ID";

    // Các Extra mới cho luồng Fail
    public static final String EXTRA_IS_FAIL_REPORT = "EXTRA_IS_FAIL_REPORT";
    public static final String EXTRA_FAIL_REASON = "EXTRA_FAIL_REASON";

    private static final String TAG = "ProofActivity";
    private static final int MAX_MEDIA = 6;

    private List<ImageView> imageViews;
    private Uri[] selectedMedia = new Uri[MAX_MEDIA];
    private int currentSlotIndex = -1;
    private Uri currentPhotoUri;
    private Uri currentVideoUri;

    private String assignmentId;
    private String parcelId;
    private String driverId;

    // Biến cho luồng Fail
    private boolean isFailReport = false;
    private String failReason;

    private ProgressDialog progressDialog;
    private Button btnSubmit;

    private ActivityResultLauncher<Intent> pickMediaLauncher;
    private ActivityResultLauncher<Intent> takeVideoLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_grid);

        // Get task info from intent
        Intent intent = getIntent();
        assignmentId = intent.getStringExtra(EXTRA_ASSIGNMENT_ID);
        parcelId = intent.getStringExtra(EXTRA_PARCEL_ID);
        driverId = intent.getStringExtra(EXTRA_DRIVER_ID);

        // Get fail report info
        isFailReport = intent.getBooleanExtra(EXTRA_IS_FAIL_REPORT, false);
        failReason = intent.getStringExtra(EXTRA_FAIL_REASON);

        // Prefer assignmentId, but fallback to parcelId + driverId for backward compatibility
        if (assignmentId == null || assignmentId.isEmpty()) {
            if (parcelId == null || driverId == null) {
                // Get driverId from SessionManager if not provided
                if (driverId == null) {
                    SessionManager sessionManager = new SessionManager(this);
                    driverId = sessionManager.getDriverId();
                }

                if (parcelId == null || driverId == null) {
                    Toast.makeText(this, "Thiếu thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
        }

        initViews();
        setupLaunchers();
        setupListeners();
        updateSubmitButtonState();

        // Cập nhật text nút bấm dựa trên chế độ
        if (isFailReport) {
            btnSubmit.setText("XÁC NHẬN THẤT BẠI");
            btnSubmit.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            btnSubmit.setText("XÁC NHẬN HOÀN THÀNH");
            btnSubmit.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void initViews() {
        imageViews = new ArrayList<>();
        imageViews.add(findViewById(R.id.slot1).findViewById(R.id.imgSlot));
        imageViews.add(findViewById(R.id.slot2).findViewById(R.id.imgSlot));
        imageViews.add(findViewById(R.id.slot3).findViewById(R.id.imgSlot));
        imageViews.add(findViewById(R.id.slot4).findViewById(R.id.imgSlot));
        imageViews.add(findViewById(R.id.slot5).findViewById(R.id.imgSlot));
        imageViews.add(findViewById(R.id.slot6).findViewById(R.id.imgSlot));

        btnSubmit = findViewById(R.id.btnConfirmProof);
    }

    private void setupLaunchers() {
        // Pick media from gallery (images or videos)
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            setMediaToCurrentSlot(uri);
                        }
                    }
                }
        );

        // Take photo
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSaved -> {
                    if (isSaved && currentPhotoUri != null) {
                        setMediaToCurrentSlot(currentPhotoUri);
                    }
                }
        );

        // Take video
        takeVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentVideoUri != null) {
                        setMediaToCurrentSlot(currentVideoUri);
                    }
                }
        );

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) showSourceDialog();
                    else Toast.makeText(this, "Cần quyền truy cập!", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void setupListeners() {
        for (int i = 0; i < imageViews.size(); i++) {
            int finalI = i;
            imageViews.get(i).setOnClickListener(v -> {
                currentSlotIndex = finalI;
                // If slot already has media, show options to replace or remove
                if (selectedMedia[finalI] != null) {
                    showMediaOptionsDialog(finalI);
                } else {
                    checkPermissionsAndShowDialog();
                }
            });
        }

        btnSubmit.setOnClickListener(v -> handleSubmit());
        findViewById(R.id.btnCancelProof).setOnClickListener(v -> finish());
    }

    private void showMediaOptionsDialog(int slotIndex) {
        String[] options = {"Thay thế", "Xóa"};
        new AlertDialog.Builder(this)
                .setTitle("Tùy chọn")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Replace
                        currentSlotIndex = slotIndex;
                        checkPermissionsAndShowDialog();
                    } else {
                        // Remove
                        selectedMedia[slotIndex] = null;
                        ImageView targetView = imageViews.get(slotIndex);
                        targetView.setImageResource(android.R.drawable.ic_menu_add);
                        targetView.setPadding(50, 50, 50, 50);
                        targetView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        updateSubmitButtonState();
                    }
                })
                .show();
    }

    private void checkPermissionsAndShowDialog() {
        String permission = Manifest.permission.CAMERA;
        // Có thể thêm check READ_MEDIA_IMAGES nếu cần mở thư viện

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            showSourceDialog();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void showSourceDialog() {
        String[] options = {"Chụp ảnh", "Quay video"};
        new AlertDialog.Builder(this)
                .setTitle("Thêm bằng chứng")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openVideoCamera();
                })
                .show();
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            currentPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePhotoLauncher.launch(currentPhotoUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*,video/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        pickMediaLauncher.launch(intent);
    }

    private void openVideoCamera() {
        try {
            File videoFile = createVideoFile();
            currentVideoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", videoFile);

            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentVideoUri);
            takeVideoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            takeVideoLauncher.launch(takeVideoIntent);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể tạo file video", Toast.LENGTH_SHORT).show();
        }
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        return File.createTempFile("VIDEO_" + timeStamp + "_", ".mp4", storageDir);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void setMediaToCurrentSlot(Uri uri) {
        if (currentSlotIndex != -1) {
            selectedMedia[currentSlotIndex] = uri;

            ImageView targetView = imageViews.get(currentSlotIndex);

            // Check if it's a video or image
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null && mimeType.startsWith("video/")) {
                // For video, show a play icon overlay or thumbnail
                // You might want to use VideoView or generate thumbnail
                targetView.setImageResource(android.R.drawable.ic_media_play);
                targetView.setPadding(50, 50, 50, 50);
            } else {
                // For image, display it
                targetView.setImageURI(uri);
                targetView.setPadding(0, 0, 0, 0);
            }

            targetView.setBackgroundColor(getResources().getColor(android.R.color.white));
            updateSubmitButtonState();
        }
    }

    private void updateSubmitButtonState() {
        int mediaCount = 0;
        for (Uri uri : selectedMedia) {
            if (uri != null) mediaCount++;
        }

        btnSubmit.setEnabled(mediaCount >= 1);
        if (mediaCount >= 1) {
            btnSubmit.setAlpha(1.0f);
        } else {
            btnSubmit.setAlpha(0.5f);
        }
    }

    private void handleSubmit() {
        // Validation: Bắt buộc ít nhất 1 media
        ArrayList<Uri> mediaUris = new ArrayList<>();
        for (Uri uri : selectedMedia) {
            if (uri != null) mediaUris.add(uri);
        }

        if (mediaUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất 1 ảnh/video bằng chứng!", Toast.LENGTH_LONG).show();
            return;
        }

        // Upload to Cloudinary first, then submit
        uploadMediaToCloudinaryThenSubmit(mediaUris);
    }

    private void uploadMediaToCloudinaryThenSubmit(List<Uri> mediaUris) {
        progressDialog.setMessage("Đang tải ảnh/video lên ...");
        progressDialog.show();

        CloudinaryService.getInstance()
                .uploadImages(this, mediaUris, new CloudinaryService.OnBatchUploadCallback() {
                    @Override
                    public void onComplete(List<String> successfulUrls) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Log.d(TAG, "Uploaded URLs count: " + (successfulUrls != null ? successfulUrls.size() : 0));

                            if (successfulUrls == null || successfulUrls.isEmpty()) {
                                Toast.makeText(ProofActivity.this, "Upload thất bại", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Gọi hàm submit chung
                            submitRequest(successfulUrls);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(ProofActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void submitRequest(List<String> mediaUrls) {
        progressDialog.setMessage("Đang đồng bộ dữ liệu...");
        progressDialog.show();

        RouteInfo routeInfoObj = RouteInfo.builder()
                .distanceM(1000) // TODO: Get actual distance
                .durationS(1000) // TODO: Get actual duration
                .waypoints("{}")
                .build();

        SessionClient service = RetrofitClient.getRetrofitInstance(this).create(SessionClient.class);
        Call<BaseResponse<DeliveryAssignment>> call;

        if (isFailReport) {
            // --- LOGIC GỌI API FAIL ---
            TaskFailRequest failRequest = new TaskFailRequest();
            failRequest.setReason(failReason);
            failRequest.setRouteInfo(routeInfoObj);
            failRequest.setProofImageUrls(mediaUrls);
            attachCurrentLocationToCompleteRequest(failRequest);

            Log.d(TAG, "Submitting FAILURE - ParcelId: " + parcelId + ", Reason: " + failReason);

            call = service.failTask(assignmentId, failRequest);

        } else {
            // --- LOGIC GỌI API COMPLETE ---
            CompleteTaskRequest completeRequest = new CompleteTaskRequest();
            completeRequest.setRouteInfo(routeInfoObj);
            completeRequest.setProofImageUrls(mediaUrls);
            attachCurrentLocationToCompleteRequest(completeRequest);

            Log.d(TAG, "Submitting COMPLETION - AssignmentId: " + assignmentId);

            call = service.completeTaskByAssignmentId(assignmentId, completeRequest);
        }

        call.enqueue(new Callback<BaseResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliveryAssignment>> call,
                                   Response<BaseResponse<DeliveryAssignment>> response) {
                progressDialog.dismiss();

                if (response.isSuccessful()) {
                    BaseResponse<DeliveryAssignment> body = response.body();
                    if (body != null) {
                        String msg = isFailReport ? "Đã báo cáo thất bại!" : "Giao hàng thành công!";
                        Toast.makeText(ProofActivity.this, msg, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(ProofActivity.this, "Phản hồi từ server không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ProofActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliveryAssignment>> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(ProofActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attachCurrentLocationToCompleteRequest(CompleteTaskRequest request) {
        Location location = getLastKnownLocation();
        if (location != null) {
            request.setCurrentLat(location.getLatitude());
            request.setCurrentLon(location.getLongitude());
            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
            request.setCurrentTimestamp(timestamp);
        }
    }

    private void attachCurrentLocationToCompleteRequest(TaskFailRequest request) {
        Location location = getLastKnownLocation();
        if (location != null) {
            request.setCurrentLat(location.getLatitude());
            request.setCurrentLon(location.getLongitude());
            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
            request.setCurrentTimestamp(timestamp);
        }
    }

    private Location getLastKnownLocation() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager == null) return null;

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation == null) {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            return lastLocation;
        } catch (Exception e) {
            Log.w(TAG, "Failed to get location: " + e.getMessage());
            return null;
        }
    }
}