package com.ds.deliveryapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ds.deliveryapp.adapter.ConversationAdapter;
import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.ChatClient;
import com.ds.deliveryapp.clients.res.Conversation;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.repository.ChatHistoryRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity hiển thị danh sách conversations của user
 */
public class ConversationsListActivity extends AppCompatActivity {

    private static final String TAG = "ConversationsListActivity";

    private RecyclerView rvConversations;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton btnBack;
    private ConversationAdapter mAdapter;
    private List<Conversation> mConversations = new ArrayList<>();

    private ChatClient mChatClient;
    private AuthManager mAuthManager;
    private String mCurrentUserId;
    private GlobalChatService globalChatService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations_list);

        mAuthManager = new AuthManager(this);
        mCurrentUserId = mAuthManager.getUserId();

        if (mCurrentUserId == null || mCurrentUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found. Please login.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize GlobalChatService
        globalChatService = GlobalChatService.getInstance(this);
        
        initViews();
        initRetrofitClients();
        initRecyclerView();
        setupGlobalChatListener();
        loadConversations();
    }

    private void initViews() {
        rvConversations = findViewById(R.id.rv_conversations);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadConversations();
        });
    }

    private void initRetrofitClients() {
        mChatClient = RetrofitClient.getChatRetrofitInstance(this).create(ChatClient.class);
    }

    private void initRecyclerView() {
        mAdapter = new ConversationAdapter(mConversations, conversation -> {
            // Click vào conversation -> mở ChatActivity
            openChatActivity(conversation);
        });

        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(mAdapter);
    }

    private void loadConversations() {
        Log.d(TAG, "📥 Loading conversations for user: " + mCurrentUserId);
        
        // Load conversations with messages included (only on initial load)
        // This will populate local database via GlobalChatService
        boolean includeMessages = mConversations.isEmpty(); // Only include messages on first load

        Call<com.ds.deliveryapp.clients.res.BaseResponse<List<Conversation>>> call = 
                mChatClient.getConversations(mCurrentUserId, includeMessages, 50);
        call.enqueue(new Callback<com.ds.deliveryapp.clients.res.BaseResponse<List<Conversation>>>() {
            @Override
            public void onResponse(@NonNull Call<com.ds.deliveryapp.clients.res.BaseResponse<List<Conversation>>> call, @NonNull Response<com.ds.deliveryapp.clients.res.BaseResponse<List<Conversation>>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    com.ds.deliveryapp.clients.res.BaseResponse<List<Conversation>> baseResponse = response.body();
                    if (baseResponse.getResult() != null) {
                        List<Conversation> conversations = baseResponse.getResult();
                        Log.d(TAG, "✅ Loaded " + conversations.size() + " conversations");

                        mConversations.clear();
                        mConversations.addAll(conversations);
                        
                        // If messages are included, save them to local database and in-memory store
                        if (includeMessages && globalChatService != null) {
                            ChatHistoryRepository chatHistoryRepo = new ChatHistoryRepository(ConversationsListActivity.this);
                            for (Conversation conv : conversations) {
                                // Store conversation metadata
                                globalChatService.setConversationMetadata(conv);
                                
                                if (conv.getMessages() != null && !conv.getMessages().isEmpty()) {
                                    // Convert Conversation.Message to Message and save
                                    List<com.ds.deliveryapp.clients.res.Message> messages = conv.getMessages();
                                    
                                    // Save to local database
                                    chatHistoryRepo.saveMessages(messages);
                                    
                                    // Store in GlobalChatService in-memory store
                                    globalChatService.setMessagesForConversation(conv.getConversationId(), messages);
                                    
                                    Log.d(TAG, "💾 Saved " + messages.size() + " messages for conversation " + conv.getConversationId() + " (DB + memory)");
                                }
                            }
                        } else if (globalChatService != null) {
                            // Even if not including messages, store conversation metadata
                            globalChatService.setConversationsMetadata(conversations);
                        }
                        
                        // Sync unread counts with GlobalChatService
                        if (globalChatService != null) {
                            java.util.Map<String, Integer> unreadCounts = new java.util.HashMap<>();
                            for (Conversation conv : conversations) {
                                if (conv.getConversationId() != null && conv.getUnreadCount() != null && conv.getUnreadCount() > 0) {
                                    unreadCounts.put(conv.getConversationId(), conv.getUnreadCount());
                                }
                            }
                            globalChatService.syncUnreadCounts(unreadCounts);
                            Log.d(TAG, "Synced " + unreadCounts.size() + " unread counts with GlobalChatService");
                        }
                        
                        mAdapter.notifyDataSetChanged();
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Không thể tải danh sách cuộc trò chuyện";
                        Log.e(TAG, "❌ Error response: " + errorMsg);
                        Toast.makeText(ConversationsListActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "❌ Failed to load conversations: " + response.code());
                    Toast.makeText(ConversationsListActivity.this, 
                            "Failed to load conversations", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.ds.deliveryapp.clients.res.BaseResponse<List<Conversation>>> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "❌ Network error loading conversations", t);
                Toast.makeText(ConversationsListActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Setup listener for GlobalChatService to update unread count in real-time
     */
    private void setupGlobalChatListener() {
        GlobalChatService.GlobalChatListener listener = new GlobalChatService.GlobalChatListener() {
            @Override
            public void onMessageReceived(com.ds.deliveryapp.clients.res.Message message) {
                // Update unread count for the conversation in the list
                if (message != null && message.getConversationId() != null) {
                    updateUnreadCountForConversation(message.getConversationId());
                }
            }

            @Override
            public void onUnreadCountChanged(int count) {
                // Update UI when unread count changes
                runOnUiThread(() -> {
                    // Update conversations list with latest unread counts from GlobalChatService
                    for (Conversation conv : mConversations) {
                        if (conv.getConversationId() != null) {
                            int unreadCount = globalChatService.getUnreadCountForConversation(conv.getConversationId());
                            conv.setUnreadCount(unreadCount);
                        }
                    }
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onConnectionStatusChanged(boolean connected) {
                // Handle connection status if needed
            }

            @Override
            public void onError(String error) {
                // Handle error if needed
            }

            @Override
            public void onNotificationReceived(String notificationJson) {
                // Handle notifications if needed
            }

            @Override
            public void onUserStatusUpdate(String userId, boolean isOnline) {
                // Update online status for conversations in the list
                runOnUiThread(() -> {
                    for (Conversation conv : mConversations) {
                        if (conv.getPartnerId() != null && conv.getPartnerId().equals(userId)) {
                            conv.setPartnerOnline(isOnline);
                            if (mAdapter != null) {
                                int position = mConversations.indexOf(conv);
                                if (position >= 0) {
                                    mAdapter.notifyItemChanged(position);
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onTypingIndicatorUpdate(String userId, String conversationId, boolean isTyping) {
                // Typing indicators are not shown in conversations list
                // This is handled in ChatActivity instead
            }
        };
        
        globalChatService.addListener(listener);
    }

    /**
     * Update unread count for a specific conversation
     */
    private void updateUnreadCountForConversation(String conversationId) {
        if (conversationId == null || globalChatService == null) return;
        
        runOnUiThread(() -> {
            int unreadCount = globalChatService.getUnreadCountForConversation(conversationId);
            for (Conversation conv : mConversations) {
                if (conversationId.equals(conv.getConversationId())) {
                    conv.setUnreadCount(unreadCount);
                    if (mAdapter != null) {
                        int position = mConversations.indexOf(conv);
                        if (position >= 0) {
                            mAdapter.notifyItemChanged(position);
                        }
                    }
                    break;
                }
            }
        });
    }

    private void openChatActivity(Conversation conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("RECIPIENT_ID", conversation.getPartnerId());
        intent.putExtra("RECIPIENT_NAME", conversation.getPartnerName());
        
        // Add parcel info if available
        if (conversation.getCurrentParcelId() != null) {
            intent.putExtra("PARCEL_ID", conversation.getCurrentParcelId());
        }
        if (conversation.getCurrentParcelCode() != null) {
            intent.putExtra("PARCEL_CODE", conversation.getCurrentParcelCode());
        }
        
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload conversations khi quay lại màn hình
        loadConversations();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener when activity is destroyed
        if (globalChatService != null) {
            // Note: We can't remove a specific listener, but GlobalChatService should handle cleanup
            // In a production app, you'd want to keep track of the listener reference
        }
    }
}
