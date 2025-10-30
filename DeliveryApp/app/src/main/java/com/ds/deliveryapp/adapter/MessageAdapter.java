package com.ds.deliveryapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.R;
import com.ds.deliveryapp.clients.res.Message;

// import com.bumptech.glide.Glide;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Message> messageList;
    private String currentUserId;
    private String mRecipientAvatarUrl;

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceiverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            // Tin nhắn GỬI (chỉ nội dung)
            ((SentViewHolder) holder).bind(message);

        } else {
            // Tin nhắn NHẬN (cần nội dung, tên và avatar đối tác)
            ((ReceiverViewHolder) holder).bind(message, mRecipientAvatarUrl);
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

    /* --- VIEWHOLDERS --- */

    // 1. ViewHolder cho tin nhắn GỬI (Sent)
    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ TextView từ R.layout.item_message_sent
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
        }

        public void bind(Message message) {
            tvMessageContent.setText(message.getContent());
            // Thêm logic hiển thị thời gian gửi nếu có
        }
    }

    // 2. ViewHolder cho tin nhắn NHẬN (Received) - CÓ AVATAR & NAME
    static class ReceiverViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent;
        ImageView ivPartnerAvatar; // Avatar

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các views từ R.layout.item_message_received
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
            ivPartnerAvatar = itemView.findViewById(R.id.iv_partner_avatar);
        }

        public void bind(Message message, String avatarUrl) {
            // Hiển thị nội dung tin nhắn
            tvMessageContent.setText(message.getContent());

            // Hiển thị Avatar
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // **HÃY SỬ DỤNG THƯ VIỆN TẢI ẢNH CỦA BẠN TẠI ĐÂY (VÍ DỤ GLIDE):**

                /*
                Glide.with(itemView.getContext())
                     .load(avatarUrl)
                     .placeholder(R.drawable.ic_default_avatar) // Ảnh chờ tải
                     .error(R.drawable.ic_default_avatar)      // Ảnh lỗi
                     .into(ivPartnerAvatar);
                */

                // Tạm thời hiển thị Avatar mặc định nếu không dùng thư viện:
                ivPartnerAvatar.setImageResource(R.drawable.ic_person);
            } else {
                ivPartnerAvatar.setImageResource(R.drawable.ic_person);
            }
        }
    }
}