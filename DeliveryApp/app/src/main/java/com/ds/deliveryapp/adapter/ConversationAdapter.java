package com.ds.deliveryapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.deliveryapp.R;
import com.ds.deliveryapp.clients.res.Conversation;

import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    private List<Conversation> conversations;
    private OnConversationClickListener listener;

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation, listener);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvPartnerName;
        TextView tvLastMessage;
        TextView tvUnreadCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvPartnerName = itemView.findViewById(R.id.tv_partner_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
        }

        void bind(Conversation conversation, OnConversationClickListener listener) {
            // Set partner name
            String displayName = conversation.getPartnerName() != null 
                    ? conversation.getPartnerName() 
                    : (conversation.getPartnerId() != null ? conversation.getPartnerId() : "Unknown");
            
            // Add online status indicator to name if online
            if (conversation.getPartnerOnline() != null && conversation.getPartnerOnline()) {
                displayName = "🟢 " + displayName;
            }
            
            // Add parcel code if available
            if (conversation.getCurrentParcelCode() != null && !conversation.getCurrentParcelCode().isEmpty()) {
                displayName += " (📦 " + conversation.getCurrentParcelCode() + ")";
            }
            
            tvPartnerName.setText(displayName);

            // Set avatar (default for now)
            ivAvatar.setImageResource(R.drawable.ic_person);
            // TODO: Load avatar with Glide if avatarUrl is available
            // if (conversation.getPartnerAvatar() != null) {
            //     Glide.with(itemView.getContext()).load(conversation.getPartnerAvatar())...
            // }

            // Last message preview
            if (conversation.getLastMessageContent() != null && !conversation.getLastMessageContent().isEmpty()) {
                tvLastMessage.setText(conversation.getLastMessageContent());
                tvLastMessage.setVisibility(View.VISIBLE);
            } else {
                tvLastMessage.setText("Tap to view messages");
                tvLastMessage.setVisibility(View.VISIBLE);
            }

            // Unread count
            if (conversation.getUnreadCount() != null && conversation.getUnreadCount() > 0) {
                tvUnreadCount.setText(String.valueOf(conversation.getUnreadCount()));
                tvUnreadCount.setVisibility(View.VISIBLE);
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(conversation);
                }
            });
        }
    }
}
