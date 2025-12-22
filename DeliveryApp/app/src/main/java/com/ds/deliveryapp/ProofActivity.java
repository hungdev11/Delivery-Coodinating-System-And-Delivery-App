package com.ds.deliveryapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProofActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT_IMAGES = "EXTRA_RESULT_IMAGES";
    private List<ImageView> imageViews;
    private Uri[] selectedImages = new Uri[3];
    private int currentSlotIndex = -1;
    private Uri currentPhotoUri;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_grid);

        initViews();
        setupLaunchers();
        setupListeners();
    }

    private void initViews() {
        imageViews = new ArrayList<>();
        imageViews.add(findViewById(R.id.slot1).findViewById(R.id.imgSlot));
        imageViews.add(findViewById(R.id.slot2).findViewById(R.id.imgSlot));
        imageViews.add(findViewById(R.id.slot3).findViewById(R.id.imgSlot));
    }

    private void setupLaunchers() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        setImageToCurrentSlot(result.getData().getData());
                    }
                }
        );

        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSaved -> {
                    if (isSaved && currentPhotoUri != null) {
                        setImageToCurrentSlot(currentPhotoUri);
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
                checkPermissionsAndShowDialog();
            });
        }

        findViewById(R.id.btnConfirmProof).setOnClickListener(v -> handleConfirm());
        findViewById(R.id.btnCancelProof).setOnClickListener(v -> finish());
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
        String[] options = {"Chụp ảnh", "Chọn từ thư viện"};
        new AlertDialog.Builder(this)
                .setTitle("Thêm bằng chứng")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
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
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void setImageToCurrentSlot(Uri uri) {
        if (currentSlotIndex != -1) {
            selectedImages[currentSlotIndex] = uri;

            ImageView targetView = imageViews.get(currentSlotIndex);

            // 1. Hiển thị ảnh
            targetView.setImageURI(uri);

            // 2. QUAN TRỌNG: Xóa padding để ảnh tràn viền CardView
            targetView.setPadding(0, 0, 0, 0);

            // 3. (Tùy chọn) Đổi màu nền để tránh bị lộ viền nếu ảnh trong suốt
            targetView.setBackgroundColor(getResources().getColor(android.R.color.white));
        }
    }

    private void handleConfirm() {
        // Validation: Bắt buộc ít nhất 1 ảnh
        ArrayList<String> resultPaths = new ArrayList<>();
        for (Uri uri : selectedImages) {
            if (uri != null) resultPaths.add(uri.toString());
        }

        if (resultPaths.isEmpty()) {
            Toast.makeText(this, "Vui lòng chụp ít nhất 1 ảnh bằng chứng!", Toast.LENGTH_LONG).show();
            return;
        }

        // Trả kết quả về Activity gọi nó
        Intent resultIntent = new Intent();
        resultIntent.putStringArrayListExtra(EXTRA_RESULT_IMAGES, resultPaths);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}