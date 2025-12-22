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

/**
 * Activity để chụp bằng chứng trả hàng về kho cho các đơn FAILED hoặc DELAYED
 */
public class ReturnToWarehouseActivity extends AppCompatActivity {

    public static final String EXTRA_ASSIGNMENT_ID = "EXTRA_ASSIGNMENT_ID";
    public static final String EXTRA_PARCEL_ID = "EXTRA_PARCEL_ID";
    public static final String EXTRA_DRIVER_ID = "EXTRA_DRIVER_ID";
    
    private static final String TAG = "ReturnToWarehouse";
    private static final int MAX_MEDIA = 6;
    
    private List<ImageView> imageViews;
    private Uri[] selectedMedia = new Uri[MAX_MEDIA];
    private int currentSlotIndex = -1;
    private Uri currentPhotoUri;
    private Uri currentVideoUri;
    
    private String assignmentId;
    private String parcelId;
    private String driverId;
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

        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Bằng chứng trả hàng về kho");
        }

        // Get task info from intent
        Intent intent = getIntent();
        assignmentId = intent.getStringExtra(EXTRA_ASSIGNMENT_ID);
        parcelId = intent.getStringExtra(EXTRA_PARCEL_ID);
        driverId = intent.getStringExtra(EXTRA_DRIVER_ID);
        
        if (assignmentId == null || assignmentId.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupLaunchers();
        setupListeners();
        updateSubmitButtonState();
        
        // Update title text in layout
        android.view.View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            android.widget.TextView titleTextView = rootView.findViewById(android.R.id.text1);
            if (titleTextView == null) {
                // Try to find TextView with "Bằng chứng giao hàng" text
                java.util.List<android.view.View> views = getAllChildren(rootView);
                for (android.view.View view : views) {
                    if (view instanceof android.widget.TextView) {
                        android.widget.TextView tv = (android.widget.TextView) view;
                        if ("Bằng chứng giao hàng".equals(tv.getText().toString())) {
                            tv.setText("Bằng chứng trả hàng về kho");
                            break;
                        }
                    }
                }
            } else {
                titleTextView.setText("Bằng chứng trả hàng về kho");
            }
        }
    }
    
    private java.util.List<android.view.View> getAllChildren(android.view.View v) {
        java.util.List<android.view.View> result = new java.util.ArrayList<>();
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                android.view.View child = vg.getChildAt(i);
                result.add(child);
                result.addAll(getAllChildren(child));
            }
        }
        return result;
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
        btnSubmit.setText("XÁC NHẬN TRẢ VỀ KHO");

        // Update title text
        android.widget.TextView titleView = (android.widget.TextView) findViewById(android.R.id.text1);
        if (titleView == null) {
            // Try to find by traversing the view hierarchy
            android.view.View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                titleView = rootView.findViewById(android.R.id.text1);
            }
        }
        // If still not found, we'll update it in onCreate after setContentView
        // For now, just set the activity title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Bằng chứng trả hàng về kho");
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }
    
    private void setupListeners() {
        // Setup click listeners for each slot
        for (int i = 0; i < imageViews.size(); i++) {
            final int index = i;
            imageViews.get(i).setOnClickListener(v -> handleSlotClick(index));
        }
        
        btnSubmit.setOnClickListener(v -> handleSubmit());
        findViewById(R.id.btnCancelProof).setOnClickListener(v -> finish());
    }

    private void setupLaunchers() {
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && currentPhotoUri != null) {
                        setMediaToCurrentSlot(currentPhotoUri);
                    }
                }
        );

        takeVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentVideoUri != null) {
                        setMediaToCurrentSlot(currentVideoUri);
                    }
                }
        );

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

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        showSourceDialog();
                    } else {
                        Toast.makeText(this, "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleSlotClick(int index) {
        currentSlotIndex = index;
        if (selectedMedia[index] == null) {
            checkPermissionsAndShowDialog();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Thao tác")
                    .setItems(new String[]{"Xem", "Xóa", "Thay đổi"}, (dialog, which) -> {
                        if (which == 0) {
                            // View
                            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                            viewIntent.setDataAndType(selectedMedia[index], 
                                    getContentResolver().getType(selectedMedia[index]));
                            startActivity(viewIntent);
                        } else if (which == 1) {
                            // Delete
                            selectedMedia[index] = null;
                            imageViews.get(index).setImageResource(android.R.drawable.ic_menu_add);
                            imageViews.get(index).setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                            updateSubmitButtonState();
                        } else {
                            // Replace
                            checkPermissionsAndShowDialog();
                        }
                    })
                    .show();
        }
    }

    private void checkPermissionsAndShowDialog() {
        String permission = Manifest.permission.CAMERA;
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

            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null && mimeType.startsWith("video/")) {
                targetView.setImageResource(android.R.drawable.ic_media_play);
                targetView.setPadding(50, 50, 50, 50);
            } else {
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
        ArrayList<Uri> mediaUris = new ArrayList<>();
        for (Uri uri : selectedMedia) {
            if (uri != null) mediaUris.add(uri);
        }

        if (mediaUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất 1 ảnh/video bằng chứng!", Toast.LENGTH_LONG).show();
            return;
        }

        uploadMediaToCloudinaryThenSubmit(mediaUris);
    }
    
    private void uploadMediaToCloudinaryThenSubmit(List<Uri> mediaUris) {
        progressDialog.setMessage("Đang tải ảnh/video lên Cloudinary...");
        progressDialog.show();

        CloudinaryService.getInstance()
                .uploadImages(this, mediaUris, new CloudinaryService.OnBatchUploadCallback() {
                    @Override
                    public void onComplete(List<String> successfulUrls) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Log.d(TAG, "Uploaded URLs: " + successfulUrls);

                            if (successfulUrls == null || successfulUrls.isEmpty()) {
                                Toast.makeText(ReturnToWarehouseActivity.this, "Upload thất bại", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            submitReturnToWarehouseRequest(successfulUrls);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(ReturnToWarehouseActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void submitReturnToWarehouseRequest(List<String> imageUrls) {
        progressDialog.setMessage("Đang xác nhận trả hàng về kho...");
        progressDialog.show();

        RouteInfo routeInfo = new RouteInfo();
        CompleteTaskRequest request = new CompleteTaskRequest(routeInfo, imageUrls);

        SessionClient sessionClient = RetrofitClient.getRetrofitInstance(this).create(SessionClient.class);
        Call<BaseResponse<DeliveryAssignment>> call = sessionClient.returnToWarehouse(assignmentId, request);

        call.enqueue(new Callback<BaseResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliveryAssignment>> call, Response<BaseResponse<DeliveryAssignment>> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<DeliveryAssignment> baseResponse = response.body();
                    if (baseResponse.getResult() != null) {
                        Toast.makeText(ReturnToWarehouseActivity.this, "Đã xác nhận trả hàng về kho thành công!", Toast.LENGTH_LONG).show();
                        
                        // Return success result
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("RETURNED_ASSIGNMENT_ID", assignmentId);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể xác nhận trả hàng về kho";
                        Toast.makeText(ReturnToWarehouseActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ReturnToWarehouseActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliveryAssignment>> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to return to warehouse", t);
                Toast.makeText(ReturnToWarehouseActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
