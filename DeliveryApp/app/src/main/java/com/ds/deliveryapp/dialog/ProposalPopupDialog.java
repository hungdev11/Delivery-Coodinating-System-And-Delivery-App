package com.ds.deliveryapp.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ds.deliveryapp.R;
import com.ds.deliveryapp.clients.res.InteractiveProposal;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.enums.ContentType;
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.utils.ChatWebSocketManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Popup dialog to display proposal messages.
 * Shows at top ~10% of screen with height ~20%
 */
public class ProposalPopupDialog extends Dialog {
    private static final String TAG = "ProposalPopupDialog";
    private Message proposalMessage;
    private GlobalChatService globalChatService;
    private Gson gson = new Gson();
    private boolean isReadOnly = false; // For STATUS_CHANGE_NOTIFICATION and read-only proposals

    public ProposalPopupDialog(@NonNull Context context, Message proposalMessage) {
        super(context);
        this.proposalMessage = proposalMessage;
        this.globalChatService = GlobalChatService.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_proposal_popup);

        // Set window position and size
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.10); // Top 10%
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.20); // Height 20%
            window.setAttributes(params);
        }

        setupUI();
    }

    private void setupUI() {
        if (proposalMessage == null || proposalMessage.getProposal() == null) {
            dismiss();
            return;
        }

        InteractiveProposal proposal = proposalMessage.getProposal();
        TextView tvTitle = findViewById(R.id.tv_proposal_title);
        TextView tvContent = findViewById(R.id.tv_proposal_content);
        TextView tvSenderTime = findViewById(R.id.tv_proposal_sender_time);
        Button btnAccept = findViewById(R.id.btn_proposal_accept);
        Button btnDecline = findViewById(R.id.btn_proposal_decline);
        Button btnDismiss = findViewById(R.id.btn_proposal_dismiss);

        // Parse proposal data
        String title = "";
        String content = proposalMessage.getContent();
        String senderId = proposalMessage.getSenderId();
        String sentAt = proposalMessage.getSentAt();
        
        try {
            JsonObject dataObj = gson.fromJson(proposal.getData(), JsonObject.class);
            if (dataObj != null) {
                if (dataObj.has("title")) {
                    title = dataObj.get("title").getAsString();
                }
                if (dataObj.has("content") && content == null) {
                    content = dataObj.get("content").getAsString();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing proposal data", e);
        }

        if (title.isEmpty()) {
            title = "Yêu cầu mới";
        }

        tvTitle.setText(title);
        tvContent.setText(content != null ? content : "");
        
        // Display sender and time info
        String senderTimeText = "";
        if (senderId != null) {
            // For now, show sender ID - can be enhanced to fetch sender name from User Service
            senderTimeText = "Từ: " + senderId.substring(0, Math.min(8, senderId.length()));
        }
        if (sentAt != null && !sentAt.isEmpty()) {
            try {
                // Parse datetime - handle both with and without timezone
                java.time.LocalDateTime dateTime;
                if (sentAt.contains("Z") || sentAt.contains("+") || sentAt.contains("-") && sentAt.lastIndexOf("-") > 10) {
                    // Has timezone info - parse as Instant
                    java.time.Instant instant = java.time.Instant.parse(sentAt);
                    dateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
                } else {
                    // No timezone info - parse as LocalDateTime directly
                    // Format: "2025-11-13T19:04:14" or "2025-11-13T19:04:14.123"
                    if (sentAt.contains("T")) {
                        dateTime = java.time.LocalDateTime.parse(sentAt.replaceAll("\\.\\d+$", "")); // Remove milliseconds if present
                    } else {
                        // Fallback: try to parse as date only
                        dateTime = java.time.LocalDate.parse(sentAt).atStartOfDay();
                    }
                }
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy", java.util.Locale.getDefault());
                String formattedTime = dateTime.format(formatter);
                if (!senderTimeText.isEmpty()) {
                    senderTimeText += " • " + formattedTime;
                } else {
                    senderTimeText = formattedTime;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing sentAt: " + sentAt, e);
                // Fallback: show raw date string (first 19 characters if available)
                String displayTime = sentAt.length() > 19 ? sentAt.substring(0, 19) : sentAt;
                if (!senderTimeText.isEmpty()) {
                    senderTimeText += " • " + displayTime;
                } else {
                    senderTimeText = displayTime;
                }
            }
        }
        tvSenderTime.setText(senderTimeText);

        // Check if this is a read-only notification (STATUS_CHANGE_NOTIFICATION)
        String proposalType = proposal.getType();
        if (isReadOnly || "STATUS_CHANGE_NOTIFICATION".equals(proposalType)) {
            // Read-only notification - hide action buttons, show only dismiss
            btnAccept.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            btnDismiss.setText("Đã hiểu");
            
            // Enhance content display for STATUS_CHANGE_NOTIFICATION
            if ("STATUS_CHANGE_NOTIFICATION".equals(proposalType)) {
                try {
                    JsonObject dataObj = gson.fromJson(proposal.getData(), JsonObject.class);
                    if (dataObj != null) {
                        StringBuilder enhancedContent = new StringBuilder();
                        if (dataObj.has("parcelCode")) {
                            enhancedContent.append("Đơn hàng: ").append(dataObj.get("parcelCode").getAsString()).append("\n");
                        }
                        if (dataObj.has("oldStatus") && dataObj.has("newStatus")) {
                            enhancedContent.append("Trạng thái: ")
                                    .append(dataObj.get("oldStatus").getAsString())
                                    .append(" → ")
                                    .append(dataObj.get("newStatus").getAsString());
                        }
                        if (enhancedContent.length() > 0) {
                            tvContent.setText(enhancedContent.toString());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing STATUS_CHANGE_NOTIFICATION data", e);
                }
            }
        } else {
            // Setup action buttons based on proposal type
            String actionType = proposal.getActionType();
            if (actionType == null) {
                actionType = "ACCEPT_DECLINE";
            }

            switch (actionType) {
                case "ACCEPT_DECLINE":
                    btnAccept.setVisibility(View.VISIBLE);
                    btnDecline.setVisibility(View.VISIBLE);
                    btnAccept.setOnClickListener(v -> handleProposalAction("ACCEPT"));
                    btnDecline.setOnClickListener(v -> handleProposalAction("DECLINE"));
                    break;
                case "TEXT_INPUT":
                    // For text input proposals, show input field (can be enhanced)
                    btnAccept.setText("Xác nhận");
                    btnAccept.setVisibility(View.VISIBLE);
                    btnDecline.setVisibility(View.GONE);
                    btnAccept.setOnClickListener(v -> handleProposalAction("CONFIRM"));
                    break;
                default:
                    btnAccept.setVisibility(View.VISIBLE);
                    btnDecline.setVisibility(View.GONE);
                    btnAccept.setOnClickListener(v -> handleProposalAction("ACCEPT"));
            }
        }

        btnDismiss.setOnClickListener(v -> dismiss());

        // Auto-dismiss after 30 seconds (or 10 seconds for read-only notifications)
        int dismissDelay = isReadOnly || "STATUS_CHANGE_NOTIFICATION".equals(proposalType) ? 10000 : 30000;
        getWindow().getDecorView().postDelayed(() -> {
            if (isShowing()) {
                dismiss();
            }
        }, dismissDelay);
    }

    /**
     * Set read-only mode (for notifications that don't require response)
     */
    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
    }

    private void handleProposalAction(String action) {
        if (proposalMessage == null || proposalMessage.getProposal() == null) {
            dismiss();
            return;
        }

        String proposalId = proposalMessage.getProposal().getId().toString();
        
        // Convert action to resultData format expected by backend
        String resultData;
        if ("ACCEPT".equals(action) || "CONFIRM".equals(action)) {
            JsonObject result = new JsonObject();
            result.addProperty("status", "ACCEPTED");
            // Add timestamp for when it was accepted
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                result.addProperty("acceptedAt", java.time.Instant.now().toString());
            } else {
                result.addProperty("acceptedAt", String.valueOf(System.currentTimeMillis()));
            }
            resultData = result.toString();
        } else if ("DECLINE".equals(action)) {
            JsonObject result = new JsonObject();
            result.addProperty("status", "DECLINED");
            resultData = result.toString();
        } else {
            // Fallback for other actions - wrap in JSON as well
            JsonObject result = new JsonObject();
            result.addProperty("status", action);
            resultData = result.toString();
        }

        // Use REST API to respond to proposal (same as ChatActivity)
        // This ensures proper resultData format is sent
        try {
            // Get ChatClient from GlobalChatService or create new instance
            // For now, we'll use the WebSocket quick action but with proper resultData
            ChatWebSocketManager webSocketManager = globalChatService.getWebSocketManager();
            
            if (webSocketManager != null && webSocketManager.isConnected()) {
                // Send via WebSocket with resultData
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("proposalId", proposalId);
                data.put("action", action);
                data.put("resultData", resultData);
                webSocketManager.sendQuickAction(proposalId, action, data);
                Log.d(TAG, "Sent proposal action: " + action + " with resultData: " + resultData + " for proposal: " + proposalId);
            } else {
                Log.e(TAG, "Cannot send proposal action: WebSocket not connected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending proposal action", e);
        }

        dismiss();
    }
}
