package com.ds.deliveryapp; // Ensure this matches your package

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.deliveryapp.adapter.MessageAdapter;
import com.ds.deliveryapp.auth.AuthManager; // Assuming you have this
import com.ds.deliveryapp.clients.AuthClient;
import com.ds.deliveryapp.clients.ChatClient;
import com.ds.deliveryapp.clients.req.ChatMessagePayload; // Ensure this has senderId field
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.Conversation;
import com.ds.deliveryapp.clients.res.KeycloakUserInfoDto;
import com.ds.deliveryapp.clients.res.Message; // DTO for message content
import com.ds.deliveryapp.clients.res.PageResponse; // DTO for paginated results
import com.ds.deliveryapp.configs.RetrofitClient; // Assuming you have this
import com.ds.deliveryapp.enums.ContentType; // Ensure this enum exists
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;
import ua.naiksoftware.stomp.dto.StompHeader;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    // Use your actual server IP or 10.0.2.2 for emulator localhost
    private static final String SERVER_WEBSOCKET_URL = "ws://192.168.1.6:21511/ws"; // Base URL

    // Views
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;

    // Adapter & Data
    private MessageAdapter mAdapter;
    private final List<Message> mMessages = new ArrayList<>();

    // Networking & Auth
    private StompClient mStompClient;
    private AuthClient mAuthClient;
    private ChatClient mChatClient;
    private final Gson mGson = new Gson();
    private CompositeDisposable mComposite = new CompositeDisposable();
    private AuthManager mAuthManager;

    // State Data
    private String mJwtToken;
    private String mCurrentUserId; // Fetched via API (/auth/me)
    private String mRecipientId;
    private String mRecipientName;
    private String mRecipientAvatarUrl;
    private String mConversationId; // Fetched via API

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuthManager = new AuthManager(this); // Initialize AuthManager
        mComposite = new CompositeDisposable(); // Initialize CompositeDisposable

        initViews();
        getInitialDataAndToken(); // Get token and set HARDCODED recipient

        if (!validateInitialIntentData()) { // Validate token is present
            return; // Exit if critical data missing
        }

        initRetrofitClients();
        initRecyclerView(); // Init recycler view before fetching data

        // Start chain: Get User -> Get Conv ID -> Load History & Connect WS
        getUserInfoAndProceed();

        setupSendButton();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
    }

    private void getInitialDataAndToken() {
        mJwtToken = mAuthManager.getAccessToken(); // Get stored token

        // --- HARDCODED RECIPIENT FOR TESTING ---
        String customerId = "72d01198-4a4e-4743-8cb8-038a9de9ea98"; // Example customer
        String shipperId = "62b08293-e714-45e1-9bec-a4a7e9e1bc71"; // Example shipper
        mRecipientId = customerId; // Set the recipient to the shipper ID for this test
        // --- END HARDCODED RECIPIENT ---

        // mConversationId will be fetched later based on mCurrentUserId and mRecipientId
        Log.d(TAG, "Initial Data - Recipient (Hardcoded): " + mRecipientId);
    }

    private boolean validateInitialIntentData() {
        // Only validate essential startup data (Token must exist)
        if (mJwtToken == null || mJwtToken.isEmpty()) {
            Log.e(TAG, "Initial data validation failed: Token missing.");
            showErrorToastAndFinish("Authentication token not found. Please login.");
            return false;
        }
        // Recipient is hardcoded, so no need to check Intent here
        return true;
    }

    private void initRecyclerView() {
        mAdapter = new MessageAdapter(mMessages, mCurrentUserId); // userId will be updated after API call
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true); // New messages appear at the bottom
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(mAdapter);
    }

    private void initRetrofitClients() {
        mChatClient = RetrofitClient.getChatRetrofitInstance().create(ChatClient.class);
        mAuthClient = RetrofitClient.getAuthRetrofitInstance().create(AuthClient.class);
    }

    /**
     * Fetches current user info using JWT, then proceeds to fetch conversation ID.
     */
    private void getUserInfoAndProceed() {
        if (mJwtToken == null) return; // Already validated, but good practice

        Log.d(TAG, "Fetching user info...");
        String authorizationHeader = "Bearer " + mJwtToken;
        Call<BaseResponse<KeycloakUserInfoDto>> call = mAuthClient.getUserInfo(authorizationHeader);

        call.enqueue(new Callback<BaseResponse<KeycloakUserInfoDto>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<KeycloakUserInfoDto>> call,
                                   @NonNull Response<BaseResponse<KeycloakUserInfoDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    KeycloakUserInfoDto user = response.body().getResult();
                    mCurrentUserId = user.getSub(); // Get the Keycloak subject ID

                    if (mCurrentUserId == null || mCurrentUserId.isEmpty()) {
                        handleFatalError("Failed to get User ID from token response.");
                        return;
                    }

                    Log.i(TAG, "✅ User info fetched. Current User ID: " + mCurrentUserId);
                    if (mAdapter != null) {
                        mAdapter.setCurrentUserId(mCurrentUserId); // Update adapter
                    }

                    // Now fetch the conversation ID using the fetched current user ID and hardcoded recipient
                    fetchConversationIdAndConnect();

                } else {
                    handleFatalError("Failed to fetch user info (API Error: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<KeycloakUserInfoDto>> call, @NonNull Throwable t) {
                handleFatalError("Network error fetching user info: " + t.getMessage());
            }
        });
    }

    /**
     * Fetches the Conversation ID based on current user and (hardcoded) recipient,
     * then loads history and connects WebSocket.
     */
    private void fetchConversationIdAndConnect() {
        if (mCurrentUserId == null || mRecipientId == null || mJwtToken == null) {
            Log.e(TAG, "Cannot fetch conversation ID: Missing user IDs or token.");
            handleFatalError("Cannot proceed without user information."); // Be explicit
            return;
        }
        Log.d(TAG, "Fetching conversation ID between " + mCurrentUserId + " and " + mRecipientId);
        String authorizationHeader = "Bearer " + mJwtToken;

        // Ensure ChatClient has findConversationWithPartner
        // Call<ConversationResponse> findConversationWithPartner(@Header("Authorization") String token, @Query("partnerId") String partnerId);
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

                    // --- ĐIỀU CHỈNH QUAN TRỌNG: CẬP NHẬT ADAPTER ---
                    if (mAdapter != null) {
                        mAdapter.setRecipientInfo(mRecipientAvatarUrl);
                        Log.d(TAG, "Adapter updated with Recipient Avatar/Name.");
                    }
                    // --------------------------------------------------

                    runOnUiThread(() -> {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(mRecipientName);
                        } else {
                            // ... (Xử lý Toolbar tùy chỉnh) ...
                        }
                    });

                    loadChatHistory();
                    connectWebSocket();

                } else {
                    // Handle API errors more gracefully, maybe show a message
                    Log.e(TAG, "Failed to fetch conversation ID (API Error: " + response.code() + " - " + response.message());
                    showErrorToast("Could not find or create conversation.");
                    // Don't necessarily finish the activity, user might retry
                    // handleFatalError("Failed to fetch conversation ID");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Conversation> call, @NonNull Throwable t) {
                // Network errors are often temporary
                Log.e(TAG, "Network error fetching conversation ID", t);
                showErrorToast("Network error finding conversation. Please check connection.");
                // handleFatalError("Network error fetching conversation ID: " + t.getMessage());
            }
        });
    }

    /**
     * Loads message history for the fetched mConversationId.
     */
    private void loadChatHistory() {
        if (mConversationId == null || mJwtToken == null) {
            Log.e(TAG,"Cannot load history: Conversation ID or Token is null.");
            showErrorToast("Could not load history (missing info).");
            return;
        }
        Log.d(TAG, "Loading history for Conversation ID: " + mConversationId);
        String authorizationHeader = "Bearer " + mCurrentUserId;

        // Ensure ChatClient.getChatHistory requires token header
        Call<PageResponse<Message>> call =
                mChatClient.getChatHistory(mConversationId, mCurrentUserId, 0, 50); // Load 50 initially

        call.enqueue(new Callback<PageResponse<Message>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<Message>> call,
                                   @NonNull Response<PageResponse<Message>> response) {

                if (response.isSuccessful() && response.body() != null && response.body().content() != null) {
                    List<Message> history = response.body().content();
                    Log.d(TAG, "Loaded " + history.size() + " messages.");
                    Collections.reverse(history); // Reverse if API returns newest first

                    runOnUiThread(() -> {
                        if (mAdapter != null) {
                            mAdapter.setMessages(history);
                            scrollToBottom();
                        }
                    });
                } else {
                    Log.e(TAG, "Error loading history API: " + response.code() + " " + response.message());
                    showErrorToast("Could not load chat history.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<Message>> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error loading history", t);
                showErrorToast("Network error loading history.");
            }
        });
    }

    /**
     * Connects to the WebSocket server using the real JWT token.
     */
    private void connectWebSocket() {
        if (mJwtToken == null) {
            Log.e(TAG, "Cannot connect WebSocket: Token is null.");
            handleFatalError("Authentication token missing.");
            return;
        }
        if (mStompClient != null && mStompClient.isConnected()) {
            Log.w(TAG, "WebSocket connection attempt ignored: Already connecting or connected.");
            return;
        }

        Log.d(TAG, "Connecting WebSocket to " + SERVER_WEBSOCKET_URL + " with token.");
        // Use the REAL JWT Token for authentication via Gateway/Interceptor
        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("Authorization", "Bearer " + mCurrentUserId));

        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SERVER_WEBSOCKET_URL);

        // Add heartbeats
        mStompClient.withClientHeartbeat(15000).withServerHeartbeat(15000);

        mStompClient.connect(headers); // Connect with REAL token

        // Manage lifecycle subscription
        Disposable lifecycleDisposable = mStompClient.lifecycle()
                .subscribe(
                        lifecycleEvent -> {
                            switch (lifecycleEvent.getType()) {
                                case OPENED:
                                    Log.i(TAG, "✅ STOMP Connection Opened");
                                    subscribeToMessages(); // Subscribe after connection is open
                                    break;
                                case CLOSED:
                                    Log.i(TAG, "🔌 STOMP Connection Closed");
                                    // Consider cleanup or showing a disconnected status
                                    break;
                                case ERROR:
                                    Log.e(TAG, "❌ STOMP Connection Error: ", lifecycleEvent.getException());
                                    showErrorToast("Chat connection error.");
                                    break;
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "❌ STOMP Lifecycle Throwable!", throwable);
                            showErrorToast("Chat connection failed.");
                        }
                );
        mComposite.add(lifecycleDisposable);
    }

    /**
     * Subscribes to the user-specific message queue.
     */
    private void subscribeToMessages() {
        if (mStompClient == null || !mStompClient.isConnected()) {
            Log.e(TAG, "Cannot subscribe: StompClient not connected.");
            // Maybe attempt to reconnect? connectWebSocket();
            return;
        }
        // Use the /user prefix which relies on the authenticated Principal on the server
        String destination = "/user/queue/messages";
        Log.d(TAG, "Subscribing to: " + destination);

        // Manage topic subscription
        Disposable topicDisposable = mStompClient.topic(destination)
                .subscribe(
                        stompMessage -> {
                            Log.d(TAG, "<<< Received STOMP: " + stompMessage.getPayload());
                            try {
                                Message message = mGson.fromJson(stompMessage.getPayload(), Message.class);
                                // Basic validation
                                if (message != null && message.getSenderId() != null && message.getContent() != null) {
                                    // Check if the message is from the intended recipient (optional but good)
                                    // if (!message.getSenderId().equals(mRecipientId)) {
                                    //      Log.w(TAG, "Received message from unexpected sender: " + message.getSenderId());
                                    //      // Decide whether to display it or not
                                    // }
                                    runOnUiThread(() -> {
                                        if (mAdapter != null) {
                                            mAdapter.addMessage(message);
                                            scrollToBottom();
                                        }
                                    });
                                } else {
                                    Log.w(TAG, "Received invalid message format from server.");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing received message JSON", e);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Error on STOMP topic subscription", throwable);
                            showErrorToast("Error receiving messages.");
                            // Consider trying to resubscribe after a delay
                        }
                );
        mComposite.add(topicDisposable);
    }

    /**
     * Sets up the listener for the send button.
     */
    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String content = etMessage.getText().toString().trim();
            if (!content.isEmpty()) {
                if (mStompClient != null && mStompClient.isConnected()) {
                    sendMessage(content);
                } else {
                    showErrorToast("Not connected to chat. Please wait or try again.");
                }
            }
        });
    }

    /**
     * Sends a message payload via STOMP, including the sender ID.
     */
    private void sendMessage(String content) {
        if (mCurrentUserId == null || mRecipientId == null) {
            Log.e(TAG, "Cannot send message: User IDs missing.");
            showErrorToast("Cannot send: User info missing.");
            return;
        }

        // --- INCLUDE SENDER ID IN PAYLOAD ---
        ChatMessagePayload payload = new ChatMessagePayload(content, mRecipientId);
        String jsonPayload = mGson.toJson(payload);
        String destination = "/app/chat.send"; // Matches @MessageMapping on the server

        Log.d(TAG, ">>> Sending STOMP to " + destination);

        // Manage send subscription
        Disposable sendDisposable = mStompClient.send(destination, jsonPayload)
                .subscribe(
                        () -> { // onSuccess
                            Log.d(TAG, "STOMP message sent successfully.");
                            // Optimistic UI update
                            Message selfMessage = new Message(
                                    null, mCurrentUserId, content, null, ContentType.TEXT // Ensure ContentType exists
                            );
                            runOnUiThread(() -> {
                                etMessage.setText(""); // Clear input field
                                if (mAdapter != null) {
                                    mAdapter.addMessage(selfMessage);
                                    scrollToBottom();
                                }
                            });
                        },
                        throwable -> { // onError
                            Log.e(TAG, "Error sending STOMP message", throwable);
                            showErrorToast("Failed to send message.");
                        }
                );
        mComposite.add(sendDisposable);
    }

    /**
     * Scrolls the RecyclerView to the last item smoothly.
     */
    private void scrollToBottom() {
        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            // Use post to ensure scrolling happens after layout updates
            rvMessages.post(() -> rvMessages.smoothScrollToPosition(mAdapter.getItemCount() - 1));
        }
    }

    /**
     * Handles fatal errors during setup by logging, showing a toast, and finishing the activity.
     */
    private void handleFatalError(String message) {
        Log.e(TAG, "Fatal Setup Error: " + message);
        showErrorToastAndFinish("Critical error: " + message);
    }

    /**
     * Shows a long toast message and finishes the current activity. Must be called from any thread.
     */
    private void showErrorToastAndFinish(String message){
        runOnUiThread(() -> {
            Toast.makeText(ChatActivity.this, message, Toast.LENGTH_LONG).show();
            finish(); // Close activity on fatal error
        });
    }

    /**
     * Shows a long toast message. Must be called from any thread.
     */
    private void showErrorToast(String message) {
        runOnUiThread(() -> Toast.makeText(ChatActivity.this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Disconnecting STOMP and disposing subscriptions.");
        if (mStompClient != null) {
            mStompClient.disconnect();
            mStompClient = null; // Help GC
        }
        if (mComposite != null && !mComposite.isDisposed()) {
            mComposite.dispose(); // Dispose all RxJava subscriptions
        }
    }
}