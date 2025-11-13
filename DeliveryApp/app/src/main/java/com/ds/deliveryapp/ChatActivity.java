package com.ds.deliveryapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.MessageAdapter;
import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.ChatClient;
import com.ds.deliveryapp.clients.req.ChatMessagePayload;
import com.ds.deliveryapp.clients.req.CreateProposalDTO;
import com.ds.deliveryapp.clients.req.ProposalResponseRequest;
import com.ds.deliveryapp.clients.req.ProposalUpdateDTO;
import com.ds.deliveryapp.clients.res.Conversation;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.clients.res.InteractiveProposal;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.res.ProposalTypeConfig;
import com.ds.deliveryapp.configs.RetrofitClient;
import com.ds.deliveryapp.enums.ContentType;
import com.ds.deliveryapp.service.GlobalChatService;
import com.ds.deliveryapp.utils.ChatWebSocketListener;
import com.ds.deliveryapp.utils.ChatWebSocketManager;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity này quản lý UI, gọi API (REST),
 * và lắng nghe sự kiện từ ChatWebSocketManager.
 */
public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnProposalActionListener, ChatWebSocketListener {

    private static final String TAG = "ChatActivity";
    private static final String SERVER_WEBSOCKET_URL = "wss://localweb.phuongy.works/ws/websocket";

    // Views
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnAttach;
    private ImageButton btnBack;
    private ImageView ivAvatar;
    private TextView tvRecipientName;
    private TextView tvRecipientStatus;

    // Adapter & Data
    private MessageAdapter mAdapter;
    private String mParcelCode;
    private String mParcelId;
    private final List<Message> mMessages = new ArrayList<>();
    private Calendar mSelectedStartTime;
    
    // Pagination
    private int mCurrentPage = 0;
    private boolean mIsLoadingMore = false;
    private boolean mHasMoreMessages = true;
    private static final int PAGE_SIZE = 30;

    // Networking & Auth
    private ChatWebSocketManager mWebSocketManager;
    private ChatClient mChatClient;
    private AuthManager mAuthManager;
    private GlobalChatService globalChatService;
    private GlobalChatService.GlobalChatListener globalChatListener;
    // State Data
    private String mJwtToken;
    private String mCurrentUserId;
    private List<String> mCurrentRoles = new ArrayList<>();
    private String mRecipientId;
    private String mRecipientName;
    private String mRecipientAvatarUrl;
    private String mConversationId;
    private List<ProposalTypeConfig> mAvailableProposals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuthManager = new AuthManager(this);
        
        // Initialize GlobalChatService
        globalChatService = GlobalChatService.getInstance(this);
        if (!globalChatService.isConnected()) {
            globalChatService.initialize();
        }

        initViews();

        // 1. Lấy tất cả dữ liệu (Token, UserID, Roles) từ AuthManager
        getInitialDataAndToken();

        if (!validateInitialIntentData()) { return; }

        initRetrofitClients();
        initRecyclerView();

        // 2. Cập nhật Adapter với UserID
        if (mAdapter != null) {
            mAdapter.setCurrentUserId(mCurrentUserId);
        }

        // 3. Setup global chat listener for this conversation
        setupGlobalChatListener();

        // 4. Bắt đầu chuỗi tải dữ liệu ngay lập tức
        fetchConversationIdAndConnect();
        loadAvailableProposals();

        setupSendButton();
    }
    
    /**
     * Setup listener for GlobalChatService to receive messages for this conversation
     */
    private void setupGlobalChatListener() {
        globalChatListener = new GlobalChatService.GlobalChatListener() {
            @Override
            public void onMessageReceived(Message message) {
                // Filter messages by conversation ID
                // Note: mConversationId might be null initially, so we'll also check later
                if (message != null) {
                    // If conversationId is set, filter by it
                    // Otherwise, accept all messages (will be filtered in onMessageReceivedFromGlobal)
                    boolean shouldProcess = true;
                    if (mConversationId != null && message.getConversationId() != null) {
                        shouldProcess = message.getConversationId().equals(mConversationId);
                    }
                    
                    if (shouldProcess) {
                        runOnUiThread(() -> {
                            onMessageReceivedFromGlobal(message);
                        });
                    }
                }
            }

            @Override
            public void onUnreadCountChanged(int count) {
                // Not needed in ChatActivity
            }

            @Override
            public void onConnectionStatusChanged(boolean connected) {
                runOnUiThread(() -> {
                    if (connected) {
                        Log.i(TAG, "GlobalChatService: WebSocket connected");
                    } else {
                        Log.w(TAG, "GlobalChatService: WebSocket disconnected");
                        showErrorToast("Chat connection lost. Reconnecting...");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "GlobalChatService error: " + error);
                });
            }

            @Override
            public void onNotificationReceived(String notificationJson) {
                // Handle notifications if needed
            }
        };
        
        globalChatService.addListener(globalChatListener);
    }
    
    /**
     * Handle message received from GlobalChatService
     */
    private void onMessageReceivedFromGlobal(Message message) {
        if (message != null && mAdapter != null && message.getSenderId() != null) {
            Log.d(TAG, "📥 RECEIVED MESSAGE via GlobalChatService: " + 
                  "id=" + message.getId() + 
                  ", sender=" + message.getSenderId() + 
                  ", type=" + message.getType() +
                  ", conversation=" + message.getConversationId());
            
            // Check if message belongs to current conversation
            boolean belongsToConversation = message.getConversationId() != null && 
                                           message.getConversationId().equals(mConversationId);
            
            if (belongsToConversation) {
                Log.d(TAG, "✅ Adding message to chat");
                mAdapter.addMessage(message);
                scrollToBottom();
                
                // Mark message as read when received and displayed
                // Only mark if message is not from current user and not already read
                if (message.getSenderId() != null && !message.getSenderId().equals(mCurrentUserId)) {
                    markMessageAsRead(message);
                }
            } else {
                Log.d(TAG, "⚠️ Message filtered out - belongs to different conversation. " +
                      "Expected: " + mConversationId + ", Got: " + message.getConversationId());
            }
        }
    }
    
    /**
     * Mark a single message as read
     */
    private void markMessageAsRead(Message message) {
        if (message == null || message.getId() == null || mConversationId == null) {
            return;
        }
        
        // Check if message is already read
        if ("READ".equals(message.getStatus())) {
            return;
        }
        
        // Mark as read via WebSocket - use GlobalChatService's WebSocketManager
        ChatWebSocketManager wsManager = null;
        if (globalChatService != null) {
            wsManager = globalChatService.getWebSocketManager();
        } else if (mWebSocketManager != null) {
            wsManager = mWebSocketManager;
        }
        
        if (wsManager != null && wsManager.isConnected()) {
            String[] messageIds = {message.getId()};
            wsManager.markMessagesAsRead(messageIds, mConversationId);
            Log.d(TAG, "Marked message as read: " + message.getId());
            
            // Also update GlobalChatService to decrement unread count
            if (globalChatService != null) {
                globalChatService.clearUnreadCountForConversation(mConversationId);
            }
        }
    }
    
    /**
     * Mark multiple messages as read when conversation is opened
     */
    private void markMessagesAsRead(List<Message> messages) {
        if (messages == null || messages.isEmpty() || mConversationId == null || mCurrentUserId == null) {
            return;
        }
        
        // Collect unread message IDs (messages from other users that are not read)
        List<String> unreadMessageIds = new ArrayList<>();
        for (Message message : messages) {
            if (message.getSenderId() != null 
                && !message.getSenderId().equals(mCurrentUserId) 
                && !"READ".equals(message.getStatus())) {
                unreadMessageIds.add(message.getId());
            }
        }
        
        // Mark as read via WebSocket - use GlobalChatService's WebSocketManager
        ChatWebSocketManager wsManager = null;
        if (globalChatService != null) {
            wsManager = globalChatService.getWebSocketManager();
        } else if (mWebSocketManager != null) {
            wsManager = mWebSocketManager;
        }
        
        if (!unreadMessageIds.isEmpty() && wsManager != null && wsManager.isConnected()) {
            String[] messageIdsArray = unreadMessageIds.toArray(new String[0]);
            wsManager.markMessagesAsRead(messageIdsArray, mConversationId);
            Log.d(TAG, "✅ Marked " + unreadMessageIds.size() + " messages as read");
            
            // Also update GlobalChatService to clear unread count for this conversation
            if (globalChatService != null) {
                globalChatService.clearUnreadCountForConversation(mConversationId);
            }
        } else if (unreadMessageIds.isEmpty()) {
            Log.d(TAG, "No unread messages to mark as read");
            // Still clear unread count in GlobalChatService even if no unread messages
            if (globalChatService != null && mConversationId != null) {
                globalChatService.clearUnreadCountForConversation(mConversationId);
            }
        }
    }
    
    /**
     * Mark all unread messages in conversation as read when user opens chat
     * This is called when conversation is opened to ensure all messages are marked as read
     */
    private void markAllMessagesAsRead() {
        if (mConversationId == null || mCurrentUserId == null) {
            Log.w(TAG, "Cannot mark messages as read: conversationId or userId is null");
            return;
        }
        
        Log.d(TAG, "📖 Marking all unread messages as read for conversation: " + mConversationId);
        
        // Get WebSocket manager
        ChatWebSocketManager wsManager = null;
        if (globalChatService != null) {
            wsManager = globalChatService.getWebSocketManager();
        } else if (mWebSocketManager != null) {
            wsManager = mWebSocketManager;
        }
        
        if (wsManager == null || !wsManager.isConnected()) {
            Log.w(TAG, "WebSocket not connected, will mark as read when connected");
            // Will be called again when messages are loaded
            return;
        }
        
        // Mark all messages in the current list as read
        if (!mMessages.isEmpty()) {
            markMessagesAsRead(mMessages);
        }
        
        // Also clear unread count in GlobalChatService immediately
        if (globalChatService != null) {
            globalChatService.clearUnreadCountForConversation(mConversationId);
            Log.d(TAG, "✅ Cleared unread count for conversation: " + mConversationId);
        }
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvRecipientName = findViewById(R.id.tv_recipient_name);
        tvRecipientStatus = findViewById(R.id.tv_recipient_status);
        btnAttach = findViewById(R.id.btn_attach);

        btnBack.setOnClickListener(v -> finish());
        btnAttach.setOnClickListener(v -> {
            showProposalMenu();
        });
    }

    /**
     * Lấy Token, UserID, và Roles trực tiếp từ AuthManager (SharedPreferences).
     */
    private void getInitialDataAndToken() {
        // 1. Lấy dữ liệu từ AuthManager
        mJwtToken = mAuthManager.getAccessToken();

        // (Giả sử AuthManager có 2 phương thức này, đọc từ SharedPreferences
        // mà LoginActivity đã lưu)
        mCurrentUserId = mAuthManager.getUserId();
        mCurrentRoles = mAuthManager.getRoles();

        Log.d(TAG, "Auth data loaded from Prefs. UserID: " + mCurrentUserId);

        // 2. Lấy dữ liệu từ Intent (như cũ)
        Log.d(TAG, "Reading data from Intent...");
        Intent intent = getIntent();
        mRecipientId = intent.getStringExtra("RECIPIENT_ID");
        mRecipientName = intent.getStringExtra("RECIPIENT_NAME");
        mParcelId = intent.getStringExtra("PARCEL_ID");
        mParcelCode = intent.getStringExtra("PARCEL_CODE");

        if (mRecipientId == null || mRecipientId.isEmpty()) {
            Log.e(TAG, "CRITICAL: RECIPIENT_ID is missing from Intent.");
            handleFatalError("Missing Recipient ID.");
        }

        Log.d(TAG, "Initial Data - Recipient ID: " + mRecipientId);
        Log.d(TAG, "Initial Data - Parcel Code: " + mParcelCode);
    }

    /**
     * Kiểm tra cả UserID và Roles đã được tải.
     */
    private boolean validateInitialIntentData() {
        if (mJwtToken == null || mJwtToken.isEmpty()) {
            Log.e(TAG, "Initial data validation failed: Token missing.");
            showErrorToastAndFinish("Authentication token not found. Please login.");
            return false;
        }
        if (mCurrentUserId == null || mCurrentUserId.isEmpty()) {
            Log.e(TAG, "Initial data validation failed: UserID missing.");
            showErrorToastAndFinish("User ID not found. Please login.");
            return false;
        }
        if (mCurrentRoles == null || mCurrentRoles.isEmpty()) {
            Log.e(TAG, "Initial data validation failed: Roles missing.");
            showErrorToastAndFinish("User Roles not found. Please login.");
            return false;
        }
        return true;
    }

    private void initRecyclerView() {
        mAdapter = new MessageAdapter(mMessages, mCurrentUserId);
        mAdapter.setListener(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(mAdapter);
        
        // Add scroll listener for infinite scroll
        // After reversing messages, oldest are at top, newest at bottom
        // Load more when scrolled to bottom (to load older messages)
        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // Check if scrolled to bottom (dy > 0 means scrolling down)
                if (dy > 0) {
                    int lastVisiblePosition = lm.findLastVisibleItemPosition();
                    int totalItems = mAdapter != null ? mAdapter.getItemCount() : 0;
                    
                    // Load more when reaching bottom 5 items and not already loading
                    if (lastVisiblePosition >= totalItems - 5 && !mIsLoadingMore && mHasMoreMessages) {
                        Log.d(TAG, "📜 Reached bottom, loading more older messages...");
                        loadMoreMessages();
                    }
                }
            }
        });
    }

    private void initRetrofitClients() {
        mChatClient = RetrofitClient.getChatRetrofitInstance().create(ChatClient.class);
    }
    /**
     * Lấy ID cuộc trò chuyện (gọi API /conversations/find-by-users).
     */
    private void fetchConversationIdAndConnect() {
        if (mCurrentUserId == null || mRecipientId == null) {
            handleFatalError("Cannot proceed without user information.");
            return;
        }
        Log.d(TAG, "Fetching conversation ID between " + mCurrentUserId + " and " + mRecipientId);

        Call<Conversation> call =
                mChatClient.getConversationBy2Users(mCurrentUserId, mRecipientId);

        call.enqueue(new Callback<Conversation>() {
            @Override
            public void onResponse(@NonNull Call<Conversation> call, @NonNull Response<Conversation> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getConversationId() != null) {
                    Conversation conversation = response.body();
                    mConversationId = conversation.getConversationId();
                    Log.i(TAG, "✅ Conversation ID fetched/found: " + mConversationId);

                    mRecipientName = conversation.getPartnerName();
                    mRecipientAvatarUrl = conversation.getPartnerAvatar();

                    if (mAdapter != null) {
                        mAdapter.setRecipientInfo(mRecipientAvatarUrl);
                    }

                    // --- CẬP NHẬT APP BAR TÙY CHỈNH ---
                    runOnUiThread(() -> {
                        if (tvRecipientName != null) {
                            tvRecipientName.setText(mRecipientName);
                        }

                        if (tvRecipientStatus != null) {
                            if (mParcelCode != null && !mParcelCode.isEmpty()) {
                                tvRecipientStatus.setText("Đơn hàng: " + mParcelCode);
                            } else {
                                tvRecipientStatus.setText("Đang hoạt động");
                            }
                        }
                        // (Thêm code Glide/Picasso để tải ivAvatar tại đây)
                    });

                    loadChatHistory();
                    
                    // Mark all messages as read when conversation is opened
                    // This ensures unread count is cleared immediately
                    markAllMessagesAsRead();
                    
                    // WebSocket is already connected via GlobalChatService
                    // Just ensure we have the manager reference
                    if (globalChatService.isConnected()) {
                        mWebSocketManager = globalChatService.getWebSocketManager();
                    } else {
                        connectWebSocket(); // Will initialize GlobalChatService if needed
                    }

                } else {
                    Log.e(TAG, "Failed to fetch conversation ID (API Error: " + response.code() + ")");
                    showErrorToast("Could not find or create conversation.");
                }
            }
            @Override
            public void onFailure(@NonNull Call<Conversation> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error fetching conversation ID", t);
            }
        });
    }

    /**
     * Tải lịch sử chat - Page đầu tiên (Initial load)
     * ✅ Server trả về messages DESC (mới nhất đầu tiên)
     * ✅ RecyclerView hiển thị theo thứ tự nhận được (DESC) với stackFromEnd=true
     */
    private void loadChatHistory() {
        if (mConversationId == null) return;
        
        Log.d(TAG, "📥 Loading initial chat history (page 0) for conversation: " + mConversationId);
        
        mCurrentPage = 0;
        mHasMoreMessages = true;
        
        Call<PageResponse<Message>> call =
                mChatClient.getChatHistory(mConversationId, mCurrentUserId, 0, PAGE_SIZE);
        call.enqueue(new Callback<PageResponse<Message>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<Message>> call, @NonNull Response<PageResponse<Message>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().content() != null) {
                    PageResponse<Message> pageResponse = response.body();
                    List<Message> history = pageResponse.content();

                    // Check if there are more pages
                    mHasMoreMessages = !pageResponse.last();
                    
                    if (!history.isEmpty()) {
                        // Log proposal messages for debugging
                        int proposalCount = 0;
                        for (Message msg : history) {
                            if (msg.getType() == ContentType.INTERACTIVE_PROPOSAL) {
                                proposalCount++;
                            }
                        }
                        Log.d(TAG, "📊 Total proposal messages in history: " + proposalCount);
                    }
                    
                    runOnUiThread(() -> {
                        if (mAdapter != null) {
                            // Messages come sorted DESC (newest first) from backend
                            // Reverse to display oldest first (scroll down to see new messages)
                            Collections.reverse(history);
                            
                            // Sync mMessages list with adapter
                            mMessages.clear();
                            mMessages.addAll(history);
                            mAdapter.setMessages(history);
                            scrollToBottom();
                            
                            // Mark all messages as read when conversation is opened
                            markMessagesAsRead(history);
                            
                            // Also mark all messages as read (in case there are more pages)
                            markAllMessagesAsRead();
                            
                            Log.d(TAG, "✅ Initial messages displayed (reversed), total: " + history.size() + ", hasMore=" + mHasMoreMessages);
                        }
                    });
                }
            }
            @Override
            public void onFailure(@NonNull Call<PageResponse<Message>> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error loading history", t);
            }
        });
    }

    /**
     * Load more messages (Infinite scroll - khi scroll lên trên)
     */
    private void loadMoreMessages() {
        if (mConversationId == null || mIsLoadingMore || !mHasMoreMessages) {
            return;
        }
        
        mIsLoadingMore = true;
        int nextPage = mCurrentPage + 1;
        
        Log.d(TAG, "📜 Loading more messages (page " + nextPage + ")...");
        
        // Show loading indicator - use post() to defer until after scroll callback completes
        rvMessages.post(() -> {
            if (mAdapter != null) {
                mAdapter.setLoadingMore(true);
            }
        });
        
        Call<PageResponse<Message>> call =
                mChatClient.getChatHistory(mConversationId, mCurrentUserId, nextPage, PAGE_SIZE);
        call.enqueue(new Callback<PageResponse<Message>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<Message>> call, @NonNull Response<PageResponse<Message>> response) {
                mIsLoadingMore = false;
                
                if (response.isSuccessful() && response.body() != null && response.body().content() != null) {
                    PageResponse<Message> pageResponse = response.body();
                    List<Message> newMessages = pageResponse.content();
                    
                    Log.d(TAG, "✅ Loaded " + newMessages.size() + " more messages (page " + nextPage + ")");
                    
                    // Log proposal messages for debugging
                    int proposalCount = 0;
                    for (Message msg : newMessages) {
                        if (msg.getType() == ContentType.INTERACTIVE_PROPOSAL) {
                            proposalCount++;
                            Log.d(TAG, "📋 Found PROPOSAL in page " + nextPage + ": id=" + msg.getId() + 
                                  ", proposal=" + (msg.getProposal() != null ? msg.getProposal().getId() : "null"));
                        }
                    }
                    if (proposalCount > 0) {
                        Log.d(TAG, "📊 Found " + proposalCount + " proposal messages in page " + nextPage);
                    }
                    
                    if (!newMessages.isEmpty()) {
                        // Update pagination state
                        mCurrentPage = nextPage;
                        mHasMoreMessages = !pageResponse.last();
                        
                        // Messages come sorted DESC (newest first) from backend
                        // Reverse to display oldest first, then prepend older messages at the BEGINNING
                        Collections.reverse(newMessages);
                        
                        // Use post() to defer UI updates until after any scroll callbacks complete
                        rvMessages.post(() -> {
                            if (mAdapter != null) {
                                // Hide loading indicator first
                                mAdapter.setLoadingMore(false);
                                
                                // Prepend older messages at the BEGINNING of list (after reverse)
                                // Use adapter's addMessages() but need to prepend instead
                                // Get current list, prepend new messages, then set
                                List<Message> currentMessages = new ArrayList<>(mMessages);
                                currentMessages.addAll(0, newMessages); // Insert at beginning
                                mAdapter.setMessages(currentMessages);
                                
                                Log.d(TAG, "✅ Prepended " + newMessages.size() + " older messages at beginning, hasMore=" + mHasMoreMessages);
                            }
                        });
                    } else {
                        mHasMoreMessages = false;
                        Log.d(TAG, "📭 No more messages to load");
                        
                        // Hide loading indicator
                        rvMessages.post(() -> {
                            if (mAdapter != null) {
                                mAdapter.setLoadingMore(false);
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "❌ Failed to load more messages: " + response.code());
                    
                    // Hide loading indicator on error
                    rvMessages.post(() -> {
                        if (mAdapter != null) {
                            mAdapter.setLoadingMore(false);
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<PageResponse<Message>> call, @NonNull Throwable t) {
                mIsLoadingMore = false;
                
                Log.e(TAG, "❌ Network error loading more messages", t);
                
                // Hide loading indicator on error - use post() to defer
                rvMessages.post(() -> {
                    if (mAdapter != null) {
                        mAdapter.setLoadingMore(false);
                    }
                });
            }
        });
    }

    /**
     * Tải các loại proposal mà user này có thể TẠO.
     */
    private void loadAvailableProposals() {
        if (mJwtToken == null) return;

        Call<List<ProposalTypeConfig>> call = mChatClient.getAvailableConfigs(mCurrentRoles);
        call.enqueue(new Callback<List<ProposalTypeConfig>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProposalTypeConfig>> call, @NonNull Response<List<ProposalTypeConfig>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "Đã tải " + response.body().size() + " proposal khả dụng.");
                    mAvailableProposals = response.body();
                } else {
                    Log.e(TAG, "Lỗi tải proposal configs: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<ProposalTypeConfig>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi mạng khi tải proposal configs", t);
            }
        });
    }

    /**
     * Connect to WebSocket using GlobalChatService.
     * ChatActivity now uses the global WebSocket connection instead of creating its own.
     */
    private void connectWebSocket() {
        // Use GlobalChatService's WebSocket manager instead of creating a new one
        // Ensure GlobalChatService is connected
        if (!globalChatService.isConnected()) {
            Log.w(TAG, "GlobalChatService not connected. Initializing...");
            globalChatService.initialize();
            
            // Wait a bit for connection to establish
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (globalChatService.isConnected()) {
                    mWebSocketManager = globalChatService.getWebSocketManager();
                    Log.d(TAG, "Using GlobalChatService WebSocket connection");
                } else {
                    Log.e(TAG, "Failed to connect GlobalChatService");
                    showErrorToast("Failed to connect to chat service.");
                }
            }, 1000);
        } else {
            // Use existing global connection
            mWebSocketManager = globalChatService.getWebSocketManager();
            Log.d(TAG, "Using existing GlobalChatService WebSocket connection");
        }
        
        // Note: We no longer create our own WebSocket connection
        // Messages are received via GlobalChatService listener (setupGlobalChatListener)
    }

    /**
     * Logic gửi tin nhắn TEXT (Chat cũ).
     */
    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString().trim();
            if (!content.isEmpty()) {
                if (mWebSocketManager != null && mWebSocketManager.isConnected()) {
                    sendMessage(content);
                } else {
                    showErrorToast("Not connected to chat. Please wait or try again.");
                }
            }
        });
    }

    /**
     * Gửi tin nhắn bằng WebSocket Manager.
     */
    private void sendMessage(String content) {
        if (mCurrentUserId == null || mRecipientId == null) {
            showErrorToast("Cannot send: User info missing.");
            return;
        }

        ChatMessagePayload payload = new ChatMessagePayload(content, mRecipientId);

        mWebSocketManager.sendMessage(payload, new ChatWebSocketManager.SendMessageCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "📤 STOMP message sent successfully - waiting for WebSocket echo...");
                
                // ✅ FIXED: Don't add optimistic message - wait for WebSocket to deliver it
                // The server will echo back the saved message with proper ID and sentAt
                // This follows the same pattern as web client
                
                runOnUiThread(() -> {
                    etMessage.setText("");
                    // Message will appear when WebSocket delivers it via onMessageReceived()
                });
                
                // Optional: Set a timeout to reload if WebSocket doesn't deliver
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (mAdapter != null) {
                        int messageCount = mAdapter.getItemCount();
                        // If no new message arrived, reload history
                        Log.d(TAG, "⏱️ Timeout check: Current message count = " + messageCount);
                    }
                }, 3000); // Wait 3 seconds for WebSocket delivery
            }
            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "❌ Error sending STOMP message", throwable);
                runOnUiThread(() -> showErrorToast("Failed to send message."));
            }
        });
    }

    /**
     * Hiển thị menu khi bấm nút +
     */
    private void showProposalMenu() {
        if (mAvailableProposals == null || mAvailableProposals.isEmpty()) {
            showErrorToast("Không có hành động nào.");
            return;
        }

        CharSequence[] items = new CharSequence[mAvailableProposals.size()];
        for(int i = 0; i < mAvailableProposals.size(); i++) {
            items[i] = mAvailableProposals.get(i).getDescription();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn hành động")
                .setItems(items, (dialog, which) -> {
                    ProposalTypeConfig selectedConfig = mAvailableProposals.get(which);
                    String actionType = selectedConfig.getCreationActionType();
                    if (actionType == null) return;
                    if ("DATE_PICKER".equals(actionType)) {
                        actionType = "POSTPONE_OPTIONS";
                    }

                    switch (actionType) {
                        case "POSTPONE_OPTIONS":
                            showPostponeOptionsDialog(selectedConfig);
                            break;
                        case "TEXT_INPUT":
                            showTextInputDialog(selectedConfig);
                            break;
                        case "ACCEPT_DECLINE":
                        default:
                            sendProposalRequest(selectedConfig.getType(), "{}", selectedConfig.getDescription() + " với mã đơn hàng: " + mParcelCode);
                            break;
                    }
                })
                .show();
    }

    /**
     * (Layer 1): Hiển thị 3 lựa chọn hoãn đơn
     */
    private void showPostponeOptionsDialog(ProposalTypeConfig config) {
        CharSequence[] postponeOptions = {
                "Vào 1 thời điểm cụ thể",
                "Trước 1 thời điểm",
                "Sau 1 thời điểm",
                "Trong 1 khoảng thời gian"
        };

        new AlertDialog.Builder(this)
                .setTitle("Chọn kiểu hoãn đơn")
                .setItems(postponeOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showSingleDateTimePickerDialog(config, "SPECIFIC");
                            break;
                        case 1:
                            showSingleDateTimePickerDialog(config, "BEFORE");
                            break;
                        case 2:
                            showSingleDateTimePickerDialog(config, "AFTER");
                            break;
                        case 3:
                            showDateTimeRangePickerDialog(config);
                            break;
                    }
                })
                .show();
    }

    /**
     * (Layer 2 - Option 1 & 2):
     * Xử lý chọn 1 mốc Ngày & Giờ
     */
    private void showSingleDateTimePickerDialog(ProposalTypeConfig config, String postponeType) {
        Calendar cal = Calendar.getInstance();
        mSelectedStartTime = null; // Reset

        DatePickerDialog dpd = new DatePickerDialog(this,
                (datePicker, year, month, day) -> {
                    mSelectedStartTime = Calendar.getInstance();
                    mSelectedStartTime.set(year, month, day);

                    TimePickerDialog tpd = new TimePickerDialog(this,
                            (timePicker, hour, minute) -> {
                                mSelectedStartTime.set(Calendar.HOUR_OF_DAY, hour);
                                mSelectedStartTime.set(Calendar.MINUTE, minute);

                                String readableDateTime = String.format("%02d:%02d ngày %02d/%02d/%d",
                                        hour, minute, day, month + 1, year);

                                String resultData = String.format("%d-%02d-%02dT%02d:%02d:00",
                                        year, month + 1, day, hour, minute);

                                String dataJson = "{}";
                                String fallbackContent = "";

                                if ("SPECIFIC".equals(postponeType)) {
                                    dataJson = "{\"specific_datetime\":\"" + resultData + "\"}";
                                    fallbackContent = config.getDescription() +  " với mã đơn hàng: " + mParcelCode + " vào " + readableDateTime;
                                } else if ("AFTER".equals(postponeType)) {
                                    dataJson = "{\"after_datetime\":\"" + resultData + "\"}";
                                    fallbackContent = config.getDescription() +  " với mã đơn hàng: " + mParcelCode + " sau " + readableDateTime;
                                } else {
                                    dataJson = "{\"after_datetime\":\"" + resultData + "\"}";
                                    fallbackContent = config.getDescription() +  " với mã đơn hàng: " + mParcelCode + " trước " + readableDateTime;
                                }

                                sendProposalRequest(config.getType(), dataJson, fallbackContent);
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true // 24-hour format
                    );
                    tpd.setTitle("Chọn Giờ");
                    tpd.show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dpd.setTitle("Chọn Ngày");
        dpd.show();
    }

    /**
     * (Layer 2 - Option 3):
     * Xử lý chọn 2 mốc Ngày & Giờ (Bắt đầu và Kết thúc)
     */
    private void showDateTimeRangePickerDialog(ProposalTypeConfig config) {
        mSelectedStartTime = null; // Reset

        Calendar cal = Calendar.getInstance();
        DatePickerDialog dpdStart = new DatePickerDialog(this, (dpdView, year, month, day) -> {
            mSelectedStartTime = Calendar.getInstance();
            mSelectedStartTime.set(year, month, day);

            TimePickerDialog tpdStart = new TimePickerDialog(this, (tpdView, hour, minute) -> {
                mSelectedStartTime.set(Calendar.HOUR_OF_DAY, hour);
                mSelectedStartTime.set(Calendar.MINUTE, minute);

                DatePickerDialog dpdEnd = new DatePickerDialog(this, (dpdView2, year2, month2, day2) -> {
                    Calendar selectedEndTime = Calendar.getInstance();
                    selectedEndTime.set(year2, month2, day2);

                    TimePickerDialog tpdEnd = new TimePickerDialog(this, (tpdView2, hour2, minute2) -> {
                        selectedEndTime.set(Calendar.HOUR_OF_DAY, hour2);
                        selectedEndTime.set(Calendar.MINUTE, minute2);

                        if (selectedEndTime.before(mSelectedStartTime)) {
                            showErrorToast("Giờ kết thúc phải sau giờ bắt đầu.");
                            return;
                        }

                        String startTimeStr = String.format("%d-%02d-%02dT%02d:%02d:00",
                                year, month + 1, day, hour, minute);
                        String endTimeStr = String.format("%d-%02d-%02dT%02d:%02d:00",
                                year2, month2 + 1, day2, hour2, minute2);

                        String dataJson = "{\"start_datetime\":\"" + startTimeStr + "\", \"end_datetime\":\"" + endTimeStr + "\"}";
                        String fallback = String.format("%s (Từ %02d:%02d %02d/%d đến %02d:%02d %02d/%d)",
                                config.getDescription(),
                                hour, minute, day, month+1,
                                hour2, minute2, day2, month2+1);

                        sendProposalRequest(config.getType(), dataJson, fallback);

                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
                    tpdEnd.setTitle("Chọn Giờ Kết Thúc");
                    tpdEnd.show();

                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                dpdEnd.setTitle("Chọn Ngày Kết Thúc");
                dpdEnd.show();

            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
            tpdStart.setTitle("Chọn Giờ Bắt Đầu");
            tpdStart.show();

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dpdStart.setTitle("Chọn Ngày Bắt Đầu");
        dpdStart.show();
    }

    /**
     * Hiển thị dialog nhập text (cho TEXT_INPUT)
     */
    private void showTextInputDialog(ProposalTypeConfig config) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(config.getDescription());
        builder.setMessage("Vui lòng nhập lý do:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20);
        input.setLayoutParams(params);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String resultData = input.getText().toString().trim();
            if (resultData.isEmpty()) {
                showErrorToast("Cần nhập lý do.");
                return;
            }
            String dataJson = "{\"reason\":\"" + resultData + "\"}";
            sendProposalRequest(config.getType(), dataJson, config.getDescription());
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Gọi API (REST) để tạo Proposal.
     */
    private void sendProposalRequest(String type, String data, String fallbackContent) {
        if (mConversationId == null || mRecipientId == null || mJwtToken == null) {
            showErrorToast("Không thể gửi yêu cầu: Thiếu thông tin.");
            return;
        }

        if ("CONFIRM_REFUSAL".equals(type) && mParcelId != null) {
            data = "{\"parcelId\":\"" + mParcelId + "\"}";
        }

        if ("POSTPONE_REQUEST".equals(type) && mParcelId != null) {
            try {
                JSONObject json = new JSONObject(data); // parse data hiện tại
                json.put("parcelId", mParcelId);        // thêm parcelId
                data = json.toString();                 // convert lại thành chuỗi JSON
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi thêm parcelId vào data", e);
                data = "{\"parcelId\":\"" + mParcelId + "\"}"; // fallback nếu lỗi
            }
        }


        CreateProposalDTO payload = new CreateProposalDTO(
                mConversationId, mRecipientId, type, data, fallbackContent,
                mCurrentUserId, mCurrentRoles
        );

        Call<InteractiveProposal> call = mChatClient.createProposal(payload);

        call.enqueue(new Callback<InteractiveProposal>() {
            @Override
            public void onResponse(@NonNull Call<InteractiveProposal> call, @NonNull Response<InteractiveProposal> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "✅ Gửi proposal thành công. Chờ WebSocket echo...");
                    // ❌ REMOVED: Don't reload entire history - WebSocket will deliver the message
                    // loadChatHistory();
                    
                    // Message with proposal will arrive via onMessageReceived()
                    runOnUiThread(() -> showErrorToast("Proposal sent! Waiting for response..."));
                } else {
                    Log.e(TAG, "❌ Gửi proposal thất bại: " + response.code());
                    showErrorToast("Gửi yêu cầu thất bại.");
                }
            }
            @Override
            public void onFailure(@NonNull Call<InteractiveProposal> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Lỗi mạng khi gửi proposal", t);
            }
        });
    }

    /* --- IMPLEMENTS TỪ ADAPTER LISTENER (Khi bấm nút) --- */

    /**
     * Được gọi từ Adapter khi bấm bất kỳ nút phản hồi nào.
     */
    @Override
    public void onProposalRespond(UUID proposalId, String resultData) {
        Log.d(TAG, "Handling RESPOND for proposal: " + proposalId + " with data: " + resultData);

        ProposalResponseRequest payload = new ProposalResponseRequest(resultData);

        Call<InteractiveProposal> call = mChatClient.respondToProposal(
                proposalId,
                mCurrentUserId,
                payload
        );

        call.enqueue(new Callback<InteractiveProposal>() {
            @Override
            public void onResponse(@NonNull Call<InteractiveProposal> call, @NonNull Response<InteractiveProposal> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "✅ Phản hồi proposal thành công. Chờ WebSocket update...");
                    // ❌ REMOVED: Don't reload entire history - WebSocket will deliver the update
                    // loadChatHistory();
                    
                    // Proposal update will arrive via onProposalUpdateReceived()
                } else {
                    Log.e(TAG, "❌ Phản hồi proposal thất bại: " + response.code());
                    showErrorToast("Thao tác thất bại.");
                }
            }
            @Override
            public void onFailure(@NonNull Call<InteractiveProposal> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Lỗi mạng khi phản hồi proposal", t);
                showErrorToast("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    /* --- IMPLEMENTS TỪ WEBSOCKET LISTENER (Legacy - now handled by GlobalChatService) --- */
    /* Note: These methods are kept for backward compatibility but messages now come via GlobalChatService */

    @Override
    public void onWebSocketOpened() {
        // Handled by GlobalChatService - this should not be called
        runOnUiThread(() -> Log.i(TAG, "ChatActivity: WebSocket Opened (via GlobalChatService)."));
    }

    @Override
    public void onWebSocketClosed() {
        // Handled by GlobalChatService - this should not be called
        runOnUiThread(() -> {
            Log.i(TAG, "ChatActivity: WebSocket Closed (via GlobalChatService).");
        });
    }

    @Override
    public void onWebSocketError(String error) {
        // Handled by GlobalChatService - this should not be called
        runOnUiThread(() -> {
            Log.e(TAG, "ChatActivity: WebSocket Error (via GlobalChatService): " + error);
        });
    }

    @Override
    public void onMessageReceived(Message message) {
        // This is now handled by onMessageReceivedFromGlobal() via GlobalChatService listener
        // Keeping this for backward compatibility but it should not be called directly
        // Messages now come through GlobalChatService.GlobalChatListener
        Log.d(TAG, "onMessageReceived called (legacy method) - forwarding to global handler");
        onMessageReceivedFromGlobal(message);
    }

    @Override
    public void onProposalUpdateReceived(ProposalUpdateDTO update) {
        // Register with GlobalChatService to receive proposal updates
        GlobalChatService.ProposalListener proposalListener = new GlobalChatService.ProposalListener() {
            @Override
            public void onProposalReceived(Message proposalMessage) {
                // Proposals are handled by MainActivity popup
                // But if it's for this conversation, also show in chat
                if (proposalMessage != null && mConversationId != null 
                    && proposalMessage.getConversationId() != null 
                    && proposalMessage.getConversationId().equals(mConversationId)) {
                    runOnUiThread(() -> {
                        onMessageReceivedFromGlobal(proposalMessage);
                    });
                }
            }

            @Override
            public void onProposalUpdate(ProposalUpdateDTO update) {
                runOnUiThread(() -> {
                    if (update != null && mAdapter != null && update.getProposalId() != null) {
                        Log.i(TAG, "Updating status for Proposal " + update.getProposalId() + " to " + update.getNewStatus());
                        String resultData = update.getResultData();
                        mAdapter.updateProposalStatus(
                                update.getProposalId(),
                                update.getNewStatus(),
                                resultData
                        );
                    }
                });
            }
        };
        globalChatService.addProposalListener(proposalListener);
        
        // Also handle direct updates (backward compatibility)
        runOnUiThread(() -> {
            if (update != null && mAdapter != null && update.getProposalId() != null) {
                Log.i(TAG, "Updating status for Proposal " + update.getProposalId() + " to " + update.getNewStatus());
                String resultData = update.getResultData();
                mAdapter.updateProposalStatus(
                        update.getProposalId(),
                        update.getNewStatus(),
                        resultData
                );
            }
        });
    }

    @Override
    public void onStatusUpdateReceived(String statusUpdateJson) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "Status update received: " + statusUpdateJson);
                // Parse the status update and update message UI
                JSONObject statusUpdate = new JSONObject(statusUpdateJson);
                String messageId = statusUpdate.optString("messageId");
                String status = statusUpdate.optString("status");
                
                if (messageId != null && !messageId.isEmpty() && mAdapter != null) {
                    mAdapter.updateMessageStatus(messageId, status);
                    Log.d(TAG, "Updated message " + messageId + " status to " + status);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing status update", e);
            }
        });
    }

    @Override
    public void onTypingIndicatorReceived(String typingIndicatorJson) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "Typing indicator received: " + typingIndicatorJson);
                // Parse typing indicator and update UI
                JSONObject typingIndicator = new JSONObject(typingIndicatorJson);
                String userId = typingIndicator.optString("userId");
                boolean isTyping = typingIndicator.optBoolean("isTyping", false);
                
                // Only show typing indicator for the other user (not self)
                if (!userId.equals(mCurrentUserId)) {
                    if (isTyping) {
                        tvRecipientStatus.setText(mRecipientName + " is typing...");
                    } else {
                        tvRecipientStatus.setText(""); // Clear typing indicator
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing typing indicator", e);
            }
        });
    }

    @Override
    public void onNotificationReceived(String notificationJson) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "Notification received: " + notificationJson);
                // Parse notification and show toast or update UI
                JSONObject notification = new JSONObject(notificationJson);
                String title = notification.optString("title");
                String content = notification.optString("content");
                
                // Show notification as toast (in-app notification)
                if (title != null && !title.isEmpty()) {
                    Toast.makeText(this, title + ": " + content, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing notification", e);
            }
        });
    }
    
    @Override
    public void onSessionMessageReceived(Message message) {
        runOnUiThread(() -> {
            Log.d(TAG, "📡 Session message received (monitoring): " + 
                  "id=" + message.getId() + 
                  ", sender=" + message.getSenderId() + 
                  ", type=" + message.getType());
            
            // For shippers: Log session messages for monitoring
            // You can add UI notifications or alerts here for important messages
            // For now, just log them
            if (message.getType() == ContentType.INTERACTIVE_PROPOSAL) {
                Log.i(TAG, "🚨 PROPOSAL from client in session: " + 
                      (message.getProposal() != null ? message.getProposal().getType() : "unknown"));
                
                // TODO: Show map popup or notification for proposals
                // This could be implemented in MapFragment or a separate monitoring UI
            }
        });
    }

    /* --- CÁC HÀM TIỆN ÍCH --- */

    private void scrollToBottom() {
        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            rvMessages.post(() -> rvMessages.smoothScrollToPosition(mAdapter.getItemCount() - 1));
        }
    }

    private void handleFatalError(String message) {
        Log.e(TAG, "Fatal Setup Error: " + message);
        showErrorToastAndFinish("Critical error: " + message);
    }

    private void showErrorToastAndFinish(String message){
        runOnUiThread(() -> {
            Toast.makeText(ChatActivity.this, message, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void showErrorToast(String message) {
        runOnUiThread(() -> Toast.makeText(ChatActivity.this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ChatActivity resumed");
        
        // When user returns to chat, mark all messages as read
        if (mConversationId != null) {
            markAllMessagesAsRead();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cleaning up ChatActivity listeners.");
        // Remove listener from GlobalChatService (but don't disconnect - it's global)
        if (globalChatService != null && globalChatListener != null) {
            globalChatService.removeListener(globalChatListener);
        }
        // Don't disconnect WebSocket - it's managed globally by GlobalChatService
        // It should stay connected even when ChatActivity is destroyed
    }
}
