package com.ds.deliveryapp.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.R;
import com.ds.deliveryapp.clients.res.InteractiveProposal;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.utils.ChatWebSocketManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Dialog for DISPUTE_APPEAL proposal type
 * Allows shipper to appeal with text message and photo evidence
 * Photos are uploaded to Cloudinary and URLs are sent in response
 */
public class DisputeAppealDialog extends Dialog {
    private static final String TAG = "DisputeAppealDialog";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/YOUR_CLOUD_NAME/image/upload";
    private static final String CLOUDINARY_UPLOAD_PRESET = "YOUR_UPLOAD_PRESET"; // Set this in Cloudinary dashboard (unsigned)

    private Message proposalMessage;
    private GlobalChatService globalChatService;
    private Gson gson = new Gson();

    private EditText etAppealMessage;
    private LinearLayout layoutSelectedImages;
    private Button btnSelectImages;
    private Button btnSubmitAppeal;
    private Button btnCancel;
    private ProgressBar progressBar;
    private TextView tvParcelInfo;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();

    public DisputeAppealDialog(@NonNull Context context, Message proposalMessage) {
        super(context);
        this.proposalMessage = proposalMessage;
        this.globalChatService = GlobalChatService.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_dispute_appeal);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        setupUI();
    }

    private void setupUI() {
        if (proposalMessage == null || proposalMessage.getProposal() == null) {
            dismiss();
            return;
        }

        etAppealMessage = findViewById(R.id.et_appeal_message);
        layoutSelectedImages = findViewById(R.id.layout_selected_images);
        btnSelectImages = findViewById(R.id.btn_select_images);
        btnSubmitAppeal = findViewById(R.id.btn_submit_appeal);
        btnCancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progress_bar);
        tvParcelInfo = findViewById(R.id.tv_parcel_info);

        // Parse parcel info from proposal data
        try {
            InteractiveProposal proposal = proposalMessage.getProposal();
            JsonObject dataObj = gson.fromJson(proposal.getData(), JsonObject.class);
            if (dataObj != null && dataObj.has("parcelCode")) {
                String parcelCode = dataObj.get("parcelCode").getAsString();
                String reason = dataObj.has("reason") ? dataObj.get("reason").getAsString() : "Khách hàng báo chưa nhận được hàng";
                tvParcelInfo.setText("Đơn hàng: " + parcelCode + "\n" + reason);
            } else {
                tvParcelInfo.setText(proposalMessage.getContent());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing proposal data", e);
            tvParcelInfo.setText(proposalMessage.getContent());
        }

        btnSelectImages.setOnClickListener(v -> openImagePicker());
        btnSubmitAppeal.setOnClickListener(v -> submitAppeal());
        btnCancel.setOnClickListener(v -> dismiss());

        progressBar.setVisibility(View.GONE);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        if (getContext() instanceof Activity) {
            ((Activity) getContext()).startActivityForResult(intent, PICK_IMAGE_REQUEST);
        }
    }

    public void onImageSelected(List<Uri> uris) {
        selectedImageUris.clear();
        selectedImageUris.addAll(uris);

        // Update UI to show selected images
        layoutSelectedImages.removeAllViews();
        for (Uri uri : uris) {
            ImageView imageView = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
            params.setMargins(10, 10, 10, 10);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(uri);
            layoutSelectedImages.addView(imageView);
        }

        if (!uris.isEmpty()) {
            btnSelectImages.setText("Đã chọn " + uris.size() + " ảnh");
        }
    }

    private void submitAppeal() {
        String message = etAppealMessage.getText().toString().trim();

        if (message.isEmpty() && selectedImageUris.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập tin nhắn hoặc chọn ảnh bằng chứng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable UI while uploading
        setUIEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Upload images to Cloudinary first, then submit appeal with URLs
        if (!selectedImageUris.isEmpty()) {
            uploadImagesToCloudinary();
        } else {
            // No images, submit immediately
            submitAppealWithUrls(message, new ArrayList<>());
        }
    }

    private void uploadImagesToCloudinary() {
        uploadedImageUrls.clear();

        // Upload images sequentially (could be parallelized for better performance)
        new Thread(() -> {
            for (int i = 0; i < selectedImageUris.size(); i++) {
                Uri uri = selectedImageUris.get(i);
                try {
                    String url = uploadImageToCloudinary(uri);
                    if (url != null) {
                        uploadedImageUrls.add(url);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error uploading image " + i, e);
                }
            }

            // All uploads done (or failed), submit appeal
            if (getContext() instanceof Activity) {
                ((Activity) getContext()).runOnUiThread(() -> {
                    String message = etAppealMessage.getText().toString().trim();
                    submitAppealWithUrls(message, uploadedImageUrls);
                });
            }
        }).start();
    }

    @Nullable
    private String uploadImageToCloudinary(Uri imageUri) {
        try {
            // Convert URI to File
            File imageFile = getFileFromUri(imageUri);
            if (imageFile == null) {
                Log.e(TAG, "Failed to convert URI to file");
                return null;
            }

            // Create multipart request
            OkHttpClient client = new OkHttpClient();
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

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                if (jsonResponse.has("secure_url")) {
                    return jsonResponse.get("secure_url").getAsString();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error uploading to Cloudinary", e);
        }
        return null;
    }

    @Nullable
    private File getFileFromUri(Uri uri) {
        try {
            Context context = getContext();
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(context.getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            OutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Error converting URI to file", e);
            return null;
        }
    }

    private void submitAppealWithUrls(String message, List<String> imageUrls) {
        if (proposalMessage == null || proposalMessage.getProposal() == null) {
            setUIEnabled(true);
            progressBar.setVisibility(View.GONE);
            dismiss();
            return;
        }

        String proposalId = proposalMessage.getProposal().getId().toString();

        // Create resultData JSON with message and image URLs
        JsonObject resultData = new JsonObject();
        resultData.addProperty("message", message);

        JsonArray urlsArray = new JsonArray();
        for (String url : imageUrls) {
            urlsArray.add(url);
        }
        resultData.add("imageUrls", urlsArray);
        resultData.addProperty("status", "ACCEPTED");

        String resultDataString = resultData.toString();

        // Send via WebSocket
        try {
            ChatWebSocketManager webSocketManager = globalChatService.getWebSocketManager();

            if (webSocketManager != null && webSocketManager.isConnected()) {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("proposalId", proposalId);
                data.put("action", "ACCEPT");
                data.put("resultData", resultDataString);
                webSocketManager.sendQuickAction(proposalId, "ACCEPT", data);
                Log.d(TAG, "Sent dispute appeal: " + resultDataString);

                Toast.makeText(getContext(), "Đã gửi kháng cáo thành công", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Cannot send appeal: WebSocket not connected");
                Toast.makeText(getContext(), "Lỗi: Không thể kết nối", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending appeal", e);
            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        setUIEnabled(true);
        progressBar.setVisibility(View.GONE);
        dismiss();
    }

    private void setUIEnabled(boolean enabled) {
        etAppealMessage.setEnabled(enabled);
        btnSelectImages.setEnabled(enabled);
        btnSubmitAppeal.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
    }
}
