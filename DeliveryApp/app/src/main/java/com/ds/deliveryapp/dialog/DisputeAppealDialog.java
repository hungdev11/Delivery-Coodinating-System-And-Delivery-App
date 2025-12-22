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

import com.ds.deliveryapp.R;
import com.ds.deliveryapp.clients.res.InteractiveProposal;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.service.CloudinaryService; // Import Service mới
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.utils.ChatWebSocketManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DisputeAppealDialog extends Dialog {
    private static final String TAG = "DisputeAppealDialog";
    private static final int PICK_IMAGE_REQUEST = 1001;

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

        displayProposalInfo();

        btnSelectImages.setOnClickListener(v -> openImagePicker());
        btnSubmitAppeal.setOnClickListener(v -> submitAppeal());
        btnCancel.setOnClickListener(v -> dismiss());

        progressBar.setVisibility(View.GONE);
    }

    private void displayProposalInfo() {
        try {
            InteractiveProposal proposal = proposalMessage.getProposal();
            if (proposal.getData() != null) {
                JsonObject dataObj = gson.fromJson(proposal.getData(), JsonObject.class);
                if (dataObj != null && dataObj.has("parcelCode")) {
                    String parcelCode = dataObj.get("parcelCode").getAsString();
                    String reason = dataObj.has("reason") ? dataObj.get("reason").getAsString() : "Khách hàng khiếu nại";
                    tvParcelInfo.setText("Đơn hàng: " + parcelCode + "\n" + reason);
                } else {
                    tvParcelInfo.setText(proposalMessage.getContent());
                }
            } else {
                tvParcelInfo.setText(proposalMessage.getContent());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing proposal data", e);
            tvParcelInfo.setText(proposalMessage.getContent());
        }
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
            Toast.makeText(getContext(), "Vui lòng nhập nội dung hoặc ảnh bằng chứng", Toast.LENGTH_SHORT).show();
            return;
        }

        setUIEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Gọi Service Upload
        CloudinaryService.getInstance().uploadImages(getContext(), selectedImageUris, new CloudinaryService.OnBatchUploadCallback() {
            @Override
            public void onComplete(List<String> successfulUrls) {
                // Upload xong (có URL hoặc list rỗng nếu không chọn ảnh), tiến hành gửi tin nhắn
                submitAppealWithUrls(message, successfulUrls);
            }

            @Override
            public void onError(String message) {
                setUIEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitAppealWithUrls(String message, List<String> imageUrls) {
        if (proposalMessage == null || proposalMessage.getProposal() == null) return;

        String proposalId = proposalMessage.getProposal().getId().toString();

        JsonObject resultData = new JsonObject();
        resultData.addProperty("message", message);

        JsonArray urlsArray = new JsonArray();
        for (String url : imageUrls) {
            urlsArray.add(url);
        }
        resultData.add("imageUrls", urlsArray);
        resultData.addProperty("status", "ACCEPTED");

        try {
            ChatWebSocketManager webSocketManager = globalChatService.getWebSocketManager();
            if (webSocketManager != null && webSocketManager.isConnected()) {
                Map<String, Object> data = new HashMap<>();
                data.put("proposalId", proposalId);
                data.put("action", "ACCEPT");
                data.put("resultData", resultData.toString());

                webSocketManager.sendQuickAction(proposalId, "ACCEPT", data);
                Toast.makeText(getContext(), "Đã gửi kháng cáo thành công", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                handleError("Không thể kết nối đến máy chủ chat");
            }
        } catch (Exception e) {
            handleError("Lỗi gửi dữ liệu: " + e.getMessage());
        }
    }

    private void handleError(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        setUIEnabled(true);
        progressBar.setVisibility(View.GONE);
    }

    private void setUIEnabled(boolean enabled) {
        etAppealMessage.setEnabled(enabled);
        btnSelectImages.setEnabled(enabled);
        btnSubmitAppeal.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
    }
}