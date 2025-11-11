package com.ds.deliveryapp.adapter;

import android.app.DatePickerDialog; // <-- IMPORT MỚI
import android.content.Context; // <-- IMPORT MỚI
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText; // <-- IMPORT MỚI
import android.widget.FrameLayout; // <-- IMPORT MỚI
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.clients.res.InteractiveProposal;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.enums.ContentType;
import com.google.gson.Gson; // <-- IMPORT MỚI
import com.google.gson.JsonObject; // <-- IMPORT MỚI

import java.util.Calendar; // <-- IMPORT MỚI
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

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_PROPOSAL_SENT = 3;
    private static final int VIEW_TYPE_PROPOSAL_RECEIVED = 4;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    public void setListener(OnProposalActionListener listener) {
        this.mListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
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
                        .inflate(R.layout.item_proposal_received_dynamic, parent, false); // <-- DÙNG LAYOUT MỚI
                return new ProposalReceiverViewHolder(view);

            case VIEW_TYPE_MESSAGE_RECEIVED:
            default:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received, parent, false);
                return new ReceiverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
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
                ((ProposalReceiverViewHolder) holder).bind(message, mRecipientAvatarUrl, mListener, mGson); // <-- TRUYỀN GSON
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    /* --- DATA MANAGEMENT METHODS --- */

    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    public void setMessages(List<Message> messages) {
        this.messageList = messages;
        notifyDataSetChanged();
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
                notifyItemChanged(i);
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
                notifyItemChanged(i);
                Log.d("MessageAdapter", "Updated message " + messageId + " status to " + newStatus);
                return;
            }
        }
    }

    /* --- VIEWHOLDERS --- */

    // 1. ViewHolder cho tin nhắn GỬI (Text)
    // (Giống file gốc của bạn)
    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
        }
        public void bind(Message message) {
            tvMessageContent.setText(message.getContent());
        }
    }

    // 2. ViewHolder cho tin nhắn NHẬN (Text)
    // (Giống file gốc của bạn)
    static class ReceiverViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;
        ImageView ivPartnerAvatar;
        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
            ivPartnerAvatar = itemView.findViewById(R.id.iv_partner_avatar);
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
}
