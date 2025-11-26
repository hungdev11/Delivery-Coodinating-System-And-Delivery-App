package com.ds.deliveryapp.adapter;

import android.app.DatePickerDialog; 
import android.content.Context; 
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText; 
import android.widget.FrameLayout; 
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.clients.res.InteractiveProposal;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.enums.ContentType;
import com.google.gson.Gson; 
import com.google.gson.JsonObject; 

import java.util.Calendar; 
import java.util.List;
import java.util.UUID;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // --- 1. INTERFACE LISTENER (ĐÃ THAY ĐỔI) ---
    public interface OnProposalActionListener {
        /**
         * @param proposalId ID của proposal
         * @param resultData Dữ liệu phản hồi (text, ngày, "ACCEPTED", "DECLINED")
         */
        void onProposalRespond(UUID proposalId, String resultData);
    }
    // --- KẾT THÚC THAY ĐỔI ---

    private List<Message> messageList;
    private String currentUserId;
    private String mRecipientAvatarUrl;
    private OnProposalActionListener mListener;
    private final Gson mGson = new Gson(); // Dùng để parse 'data' JSON
    private boolean showLoadingItem = false; // For pagination loading indicator

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_PROPOSAL_SENT = 3;
    private static final int VIEW_TYPE_PROPOSAL_RECEIVED = 4;
    private static final int VIEW_TYPE_LOADING = 5;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    public void setListener(OnProposalActionListener listener) {
        this.mListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // Check if this is the loading item (at the beginning - for loading older messages)
        if (showLoadingItem && position == 0) {
            return VIEW_TYPE_LOADING;
        }
        
        // Adjust position for message list (skip loading item at position 0)
        int messagePosition = showLoadingItem ? position - 1 : position;
        
        // Safety check
        if (messagePosition < 0 || messagePosition >= messageList.size()) {
            return VIEW_TYPE_MESSAGE_RECEIVED; // Default fallback
        }
        
        Message message = messageList.get(messagePosition);
        boolean isMine = message.getSenderId().equals(currentUserId);

        if (message.getType() == ContentType.INTERACTIVE_PROPOSAL) {
            return isMine ? VIEW_TYPE_PROPOSAL_SENT : VIEW_TYPE_PROPOSAL_RECEIVED;
        } else {
            return isMine ? VIEW_TYPE_MESSAGE_SENT : VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_MESSAGE_SENT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent, parent, false);
                return new SentViewHolder(view);

            case VIEW_TYPE_PROPOSAL_SENT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_proposal_sent, parent, false);
                return new ProposalSentViewHolder(view);

            case VIEW_TYPE_PROPOSAL_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_proposal_received_dynamic, parent, false);
                return new ProposalReceiverViewHolder(view);

            case VIEW_TYPE_LOADING:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_loading, parent, false);
                return new LoadingViewHolder(view);

            case VIEW_TYPE_MESSAGE_RECEIVED:
            default:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received, parent, false);
                return new ReceiverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Loading item doesn't need binding
        if (holder.getItemViewType() == VIEW_TYPE_LOADING) {
            return;
        }
        
        // Adjust position for message list (skip loading item at position 0)
        int messagePosition = showLoadingItem ? position - 1 : position;
        
        // Safety check
        if (messagePosition < 0 || messagePosition >= messageList.size()) {
            return; // Skip binding if position is invalid
        }
        
        Message message = messageList.get(messagePosition);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceiverViewHolder) holder).bind(message, mRecipientAvatarUrl);
                break;
            case VIEW_TYPE_PROPOSAL_SENT:
                ((ProposalSentViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_PROPOSAL_RECEIVED:
                ((ProposalReceiverViewHolder) holder).bind(message, mRecipientAvatarUrl, mListener, mGson);
                break;
        }
    }

    @Override
    public int getItemCount() {
        // Add 1 for loading item if showing
        return messageList.size() + (showLoadingItem ? 1 : 0);
    }

    /* --- DATA MANAGEMENT METHODS --- */

    /**
     * Add a new message to the list
     * - Checks for duplicates (by message ID)
     * - Maintains chronological order (sorted by sentAt ASC - oldest first, newest last)
     * - Handles proposal messages correctly
     */
    public void addMessage(Message message) {
        if (message == null || message.getId() == null) {
            Log.w("MessageAdapter", "⚠️ Cannot add null message or message without ID");
            return;
        }
        
        // Check if message already exists (by ID)
        for (int i = 0; i < messageList.size(); i++) {
            Message existing = messageList.get(i);
            if (existing.getId() != null && existing.getId().equals(message.getId())) {
                Log.d("MessageAdapter", "🔄 Message already exists, updating: " + message.getId());
                // Update existing message (in case proposal status changed, etc.)
                messageList.set(i, message);
                notifyItemChanged(i);
                return;
            }
        }
        
        // Log proposal messages for debugging
        if (message.getType() == ContentType.INTERACTIVE_PROPOSAL) {
            Log.d("MessageAdapter", "📋 Adding PROPOSAL message: id=" + message.getId() + 
                  ", proposal=" + (message.getProposal() != null ? message.getProposal().getId() : "null") +
                  ", type=" + (message.getProposal() != null ? message.getProposal().getType() : "null") +
                  ", status=" + (message.getProposal() != null ? message.getProposal().getStatus() : "null"));
        }
        
        // Insert message in correct position (maintain ASC order by sentAt - oldest first, newest last)
        int insertPosition = findInsertPosition(message);
        messageList.add(insertPosition, message);
        
        // Adjust position for RecyclerView (add 1 if loading item is showing at position 0)
        int recyclerViewPosition = insertPosition + (showLoadingItem ? 1 : 0);
        notifyItemInserted(recyclerViewPosition);
        
        Log.d("MessageAdapter", "✅ Added message at position " + insertPosition + 
              " (RecyclerView position: " + recyclerViewPosition + "), total messages: " + messageList.size());
    }
    
    /**
     * Find the correct position to insert a message to maintain ASC order by sentAt
     * Returns the index where the message should be inserted
     * After reversing, messages are ASC order (oldest first, newest last)
     */
    private int findInsertPosition(Message newMessage) {
        if (newMessage.getSentAt() == null || newMessage.getSentAt().isEmpty()) {
            // If no timestamp, add at the end (newest messages go at end)
            return messageList.size();
        }
        
        try {
            // Parse new message timestamp
            java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            String newSentAtStr = newMessage.getSentAt().substring(0, Math.min(19, newMessage.getSentAt().length()));
            long newMessageTime = isoFormat.parse(newSentAtStr).getTime();
            
            // Find position where new message should be inserted (ASC order = oldest first, newest last)
            for (int i = 0; i < messageList.size(); i++) {
                Message existing = messageList.get(i);
                if (existing.getSentAt() == null || existing.getSentAt().isEmpty()) {
                    continue; // Skip messages without timestamp
                }
                
                try {
                    String existingSentAtStr = existing.getSentAt().substring(0, Math.min(19, existing.getSentAt().length()));
                    long existingTime = isoFormat.parse(existingSentAtStr).getTime();
                    
                    // If new message is newer (larger timestamp), insert after this one
                    // If new message is older (smaller timestamp), insert before this one
                    if (newMessageTime < existingTime) {
                        return i; // Insert before older message
                    }
                } catch (Exception e) {
                    Log.e("MessageAdapter", "Error parsing existing message timestamp", e);
                }
            }
            
            // If no older message found, add at the end (newest messages go at end)
            return messageList.size();
            
        } catch (Exception e) {
            Log.e("MessageAdapter", "Error finding insert position for message", e);
            // On error, add at the end
            return messageList.size();
        }
    }

    /**
     * Set messages list - clears existing and adds new messages
     * Maintains the same list reference for proper synchronization
     */
    public void setMessages(List<Message> messages) {
        if (messages == null) {
            this.messageList.clear();
        } else {
            this.messageList.clear();
            this.messageList.addAll(messages);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Add multiple messages to the end of the list (for pagination)
     * Used when loading older messages
     */
    public void addMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        int startPosition = messageList.size();
        messageList.addAll(messages);
        notifyItemRangeInserted(startPosition, messages.size());
        
        Log.d("MessageAdapter", "✅ Added " + messages.size() + " messages at position " + startPosition + 
              ", total now: " + messageList.size());
    }
    
    /**
     * Show/hide loading indicator for pagination
     * Loading indicator is shown at the beginning (position 0) for loading older messages
     */
    public void setLoadingMore(boolean loading) {
        if (showLoadingItem == loading) return;
        
        boolean wasShowing = showLoadingItem;
        showLoadingItem = loading;
        
        if (loading && !wasShowing) {
            // Loading item added at the beginning (position 0)
            notifyItemInserted(0);
        } else if (!loading && wasShowing) {
            // Loading item removed from the beginning (position 0)
            notifyItemRemoved(0);
        }
    }

    public void setRecipientInfo(String recipientAvatarUrl) {
        this.mRecipientAvatarUrl = recipientAvatarUrl;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    /**
     * Hàm được gọi từ ChatActivity khi có WebSocket update.
     */
    public void updateProposalStatus(UUID proposalId, String newStatus, String newResultData) {
        if (messageList == null) return;

        for (int i = 0; i < messageList.size(); i++) {
            Message msg = messageList.get(i);
            if (msg.getProposal() != null && msg.getProposal().getId().equals(proposalId)) {
                msg.getProposal().setStatus(newStatus);
                msg.getProposal().setResultData(newResultData); // <-- LƯU KẾT QUẢ
                
                // Adjust position for RecyclerView (add 1 if loading item is showing at position 0)
                int recyclerViewPosition = i + (showLoadingItem ? 1 : 0);
                notifyItemChanged(recyclerViewPosition);
                return;
            }
        }
    }

    /**
     * Update message status (SENT, DELIVERED, READ)
     */
    public void updateMessageStatus(String messageId, String newStatus) {
        if (messageList == null) return;

        for (int i = 0; i < messageList.size(); i++) {
            Message msg = messageList.get(i);
            if (msg.getId() != null && msg.getId().equals(messageId)) {
                msg.setStatus(newStatus);
                
                // Adjust position for RecyclerView (add 1 if loading item is showing at position 0)
                int recyclerViewPosition = i + (showLoadingItem ? 1 : 0);
                notifyItemChanged(recyclerViewPosition);
                Log.d("MessageAdapter", "Updated message " + messageId + " status to " + newStatus);
                return;
            }
        }
    }

    /* --- VIEWHOLDERS --- */

    // 1. ViewHolder cho tin nhắn GỬI (Text)
    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;
        TextView tvTimestamp;
        TextView tvStatus;
        
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
        
        public void bind(Message message) {
            tvMessageContent.setText(message.getContent());
            
            // Format and display timestamp
            if (message.getSentAt() != null && !message.getSentAt().isEmpty()) {
                String formattedTime = formatTimestamp(message.getSentAt());
                tvTimestamp.setText(formattedTime);
                tvTimestamp.setVisibility(View.VISIBLE);
            } else {
                tvTimestamp.setVisibility(View.GONE);
            }
            
            // Display read receipt (status)
            if (message.getStatus() != null && !message.getStatus().isEmpty()) {
                String statusText = getStatusText(message.getStatus());
                tvStatus.setText(statusText);
                tvStatus.setVisibility(View.VISIBLE);
            } else {
                tvStatus.setVisibility(View.GONE);
            }
        }
        
        /**
         * Convert status to display text
         */
        private String getStatusText(String status) {
            if (status == null) return "";
            switch (status.toUpperCase()) {
                case "SENT":
                    return "✓";
                case "DELIVERED":
                    return "✓✓";
                case "READ":
                    return "✓✓";
                default:
                    return "";
            }
        }
        
        /**
         * Format ISO timestamp to HH:mm (e.g., "14:30")
         * Input format: "2024-01-15T14:30:45.123" or "2024-01-15T14:30:45"
         */
        private String formatTimestamp(String isoTimestamp) {
            try {
                // Parse the ISO timestamp
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                
                java.util.Date date = isoFormat.parse(isoTimestamp.substring(0, Math.min(19, isoTimestamp.length())));
                
                // Format to HH:mm
                java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                return displayFormat.format(date);
            } catch (Exception e) {
                Log.e("MessageAdapter", "Error formatting timestamp: " + isoTimestamp, e);
                return ""; // Return empty string if parsing fails
            }
        }
    }

    // 2. ViewHolder cho tin nhắn NHẬN (Text)
    static class ReceiverViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;
        ImageView ivPartnerAvatar;
        TextView tvTimestamp;
        
        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
            ivPartnerAvatar = itemView.findViewById(R.id.iv_partner_avatar);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }
        
        public void bind(Message message, String avatarUrl) {
            tvMessageContent.setText(message.getContent());
            
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // Tạm thời hiển thị Avatar mặc định
                ivPartnerAvatar.setImageResource(R.drawable.ic_person);
                // Bỏ comment khi dùng Glide
                // Glide.with(itemView.getContext()).load(avatarUrl)...
            } else {
                ivPartnerAvatar.setImageResource(R.drawable.ic_person);
            }
            
            // Format and display timestamp
            if (message.getSentAt() != null && !message.getSentAt().isEmpty()) {
                String formattedTime = formatTimestamp(message.getSentAt());
                tvTimestamp.setText(formattedTime);
                tvTimestamp.setVisibility(View.VISIBLE);
            } else {
                tvTimestamp.setVisibility(View.GONE);
            }
        }
        
        /**
         * Format ISO timestamp to HH:mm (e.g., "14:30")
         */
        private String formatTimestamp(String isoTimestamp) {
            try {
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                
                java.util.Date date = isoFormat.parse(isoTimestamp.substring(0, Math.min(19, isoTimestamp.length())));
                
                java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                return displayFormat.format(date);
            } catch (Exception e) {
                Log.e("MessageAdapter", "Error formatting timestamp: " + isoTimestamp, e);
                return "";
            }
        }
    }

    // 3. ViewHolder cho tin nhắn GỬI (Proposal)
    static class ProposalSentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;
        TextView tvStatus;
        TextView tvResultData; // <-- THÊM TEXT ĐỂ HIỂN THỊ KẾT QUẢ

        public ProposalSentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvResultData = itemView.findViewById(R.id.tv_result_data); // <-- Cần ID này trong layout
        }

        public void bind(Message message) {
            tvMessageContent.setText(message.getContent());
            if (message.getProposal() != null) {
                tvStatus.setText(message.getProposal().getStatus());

                String result = message.getProposal().getResultData();
                if (result != null && !result.isEmpty()) {
                    tvResultData.setVisibility(View.VISIBLE);
                    tvResultData.setText("Kết quả: " + result);
                } else {
                    tvResultData.setVisibility(View.GONE);
                }
            }
        }
    }

    // 4. ViewHolder NHẬN (Proposal) - **LOGIC MỚI**
    static class ProposalReceiverViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;
        ImageView ivPartnerAvatar;
        TextView tvStatus;
        TextView tvResultData; // <-- THÊM
        FrameLayout dynamicUiContainer; // <-- VIEW CHỨA UI ĐỘNG

        // Context để inflate layouts
        private Context mContext;

        public ProposalReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
            ivPartnerAvatar = itemView.findViewById(R.id.iv_partner_avatar);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvResultData = itemView.findViewById(R.id.tv_result_data);
            dynamicUiContainer = itemView.findViewById(R.id.dynamic_ui_container);
        }

        public void bind(Message message, String avatarUrl, OnProposalActionListener listener, Gson gson) {
            tvMessageContent.setText(message.getContent());
            ivPartnerAvatar.setImageResource(R.drawable.ic_person);
            // (Thêm Glide/Picasso)

            InteractiveProposal proposal = message.getProposal();
            if (proposal == null) return;

            // Xóa UI động cũ trước khi bind
            dynamicUiContainer.removeAllViews();

            if ("PENDING".equals(proposal.getStatus())) {
                // Đang chờ -> Ẩn status, render UI động
                tvStatus.setVisibility(View.GONE);
                tvResultData.setVisibility(View.GONE);
                dynamicUiContainer.setVisibility(View.VISIBLE);

                // Lấy actionType (Giả sử DTO InteractiveProposal đã có getActionType())
                String actionType = proposal.getActionType();
                if (actionType == null) actionType = "ACCEPT_DECLINE"; // Mặc định

                // Phân tích JSON data để lấy title
                String title = "";
                try {
                    JsonObject dataObj = gson.fromJson(proposal.getData(), JsonObject.class);
                    if (dataObj != null && dataObj.has("title")) {
                        title = dataObj.get("title").getAsString();
                    }
                } catch (Exception e) {
                    Log.e("ProposalViewHolder", "JSON data parse error", e);
                }

                // --- RENDER UI ĐỘNG ---
                switch (actionType) {
                    case "TEXT_INPUT":
                        renderTextInput(proposal, title, listener);
                        break;
                    case "DATE_PICKER":
                        renderDatePicker(proposal, title, listener);
                        break;
                    case "ACCEPT_DECLINE":
                    default:
                        renderAcceptDecline(proposal, listener);
                        break;
                }

            } else {
                // Đã xử lý (ACCEPTED, DECLINED...)
                dynamicUiContainer.setVisibility(View.GONE);
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(proposal.getStatus());

                String result = proposal.getResultData();
                if (result != null && !result.isEmpty() && !"ACCEPTED".equals(result) && !"DECLINED".equals(result)) {
                    tvResultData.setVisibility(View.VISIBLE);
                    tvResultData.setText("Kết quả: " + result);
                } else {
                    tvResultData.setVisibility(View.GONE);
                }
            }
        }

        // --- CÁC HÀM RENDER UI ĐỘNG ---

        private void renderAcceptDecline(InteractiveProposal proposal, OnProposalActionListener listener) {
            // Inflate layout R.layout.partial_action_accept_decline
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.partial_action_accept_decline, dynamicUiContainer, false);

            Button btnAccept = view.findViewById(R.id.btn_accept);
            Button btnDecline = view.findViewById(R.id.btn_decline);

            btnAccept.setOnClickListener(v -> listener.onProposalRespond(proposal.getId(), "ACCEPTED"));
            btnDecline.setOnClickListener(v -> listener.onProposalRespond(proposal.getId(), "DECLINED"));

            dynamicUiContainer.addView(view);
        }

        private void renderTextInput(InteractiveProposal proposal, String title, OnProposalActionListener listener) {
            // Inflate layout R.layout.partial_action_text_input
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.partial_action_text_input, dynamicUiContainer, false);

            EditText etInput = view.findViewById(R.id.et_input);
            Button btnSend = view.findViewById(R.id.btn_send_response);

            if (title != null && !title.isEmpty()) etInput.setHint(title);

            btnSend.setOnClickListener(v -> {
                String result = etInput.getText().toString().trim();
                if (!result.isEmpty()) {
                    listener.onProposalRespond(proposal.getId(), result);
                }
            });
            dynamicUiContainer.addView(view);
        }

        private void renderDatePicker(InteractiveProposal proposal, String title, OnProposalActionListener listener) {
            // Inflate layout R.layout.partial_action_date_picker
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.partial_action_date_picker, dynamicUiContainer, false);

            Button btnPickDate = view.findViewById(R.id.btn_pick_date);
            if (title != null && !title.isEmpty()) btnPickDate.setText(title);

            btnPickDate.setOnClickListener(v -> {
                Calendar cal = Calendar.getInstance();
                DatePickerDialog dpd = new DatePickerDialog(mContext,
                        (datePicker, year, month, day) -> {
                            // Format ngày (ví dụ: "YYYY-MM-DD")
                            String result = String.format("%d-%02d-%02d", year, month + 1, day);
                            listener.onProposalRespond(proposal.getId(), result);
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                );
                // (Bạn có thể set min_date/max_date từ proposal.getData() ở đây)
                dpd.show();
            });
            dynamicUiContainer.addView(view);
        }
    }
    
    // 5. ViewHolder cho Loading Indicator
    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            // No binding needed - just display the loading layout
        }
    }
}
