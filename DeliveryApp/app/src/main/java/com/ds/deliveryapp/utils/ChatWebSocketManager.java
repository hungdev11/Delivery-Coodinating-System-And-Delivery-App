package com.ds.deliveryapp.utils;

import android.util.Log;
import com.ds.deliveryapp.clients.req.ChatMessagePayload;
import com.ds.deliveryapp.clients.req.ProposalUpdateDTO;
import com.ds.deliveryapp.clients.res.Message;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Interceptor;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class ChatWebSocketManager {

    private static final String TAG = "ChatWebSocketManager";
    // Subscription destinations (without user ID - Spring's SimpleBroker handles user routing)
    private static final String WS_SUB_MESSAGES = "/user/queue/messages";
    private static final String WS_SUB_PROPOSAL_UPDATES = "/user/queue/proposal-updates";
    private static final String WS_SUB_STATUS_UPDATES = "/user/queue/status-updates";
    private static final String WS_SUB_TYPING = "/user/queue/typing";
    private static final String WS_SUB_NOTIFICATIONS = "/user/queue/notifications";
    // Send destinations
    private static final String WS_SEND_MESSAGE = "/app/chat.send";
    private static final String WS_SEND_TYPING = "/app/chat.typing";
    private static final String WS_SEND_READ = "/app/chat.read";
    private static final String WS_SEND_QUICK_ACTION = "/app/chat.quick-action";

    private StompClient mStompClient;
    private CompositeDisposable mComposite;
    private final Gson mGson = new Gson();
    private final String mWebSocketUrl;
    private final String mJwtToken;
    private final String mUserId;
    private ChatWebSocketListener mListener; // Listener (chính là ChatActivity)

    public ChatWebSocketManager(String webSocketUrl, String jwtToken, String userId) {
        this.mWebSocketUrl = webSocketUrl;
        this.mJwtToken = jwtToken;
        this.mUserId = userId;
    }

    public void setListener(ChatWebSocketListener listener) {
        this.mListener = listener;
    }

    public boolean isConnected() {
        return mStompClient != null && mStompClient.isConnected();
    }

    /**
     * Kết nối đến server WebSocket.
     */
    public void connect() {
        if (mUserId == null || mUserId.isEmpty()) {
            Log.e(TAG, "Cannot connect WebSocket: User ID is null or empty.");
            if (mListener != null) mListener.onWebSocketError("User ID is null or empty");
            return;
        }
        if (isConnected()) {
            Log.w(TAG, "WebSocket connection attempt ignored: Already connected.");
            return;
        }

        Log.d(TAG, "Connecting WebSocket to " + mWebSocketUrl);
        mComposite = new CompositeDisposable();

        // Create OkHttpClient with network interceptor to add Authorization header to WebSocket handshake
        // Network interceptors are called for both HTTP and WebSocket requests
        // Note: Server expects "Bearer <USER_ID>" not "Bearer <JWT_TOKEN>" per WebSocketAuthInterceptor
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request.Builder requestBuilder = originalRequest.newBuilder();
                        
                        // Add Authorization header to WebSocket handshake request
                        // Server expects userId, not JWT token (per WebSocketAuthInterceptor)
                        if (mUserId != null) {
                            String authValue = "Bearer " + mUserId;
                            String existingAuth = originalRequest.header("Authorization");
                            if (!authValue.equals(existingAuth)) {
                                requestBuilder.header("Authorization", authValue);
                                Log.d(TAG, "Adding Authorization header to WebSocket handshake: Bearer " + mUserId);
                            }
                        }
                        
                        Request newRequest = requestBuilder.build();
                        return chain.proceed(newRequest);
                    }
                })
                .build();

        // Create headers map for WebSocket handshake
        // Server expects userId, not JWT token (per WebSocketAuthInterceptor)
        Map<String, String> handshakeHeaders = new HashMap<>();
        if (mUserId != null) {
            handshakeHeaders.put("Authorization", "Bearer " + mUserId);
        }

        // Use Stomp.over() with ConnectionProvider.OKHTTP, URL, headers map, and custom OkHttpClient
        // STOMP CONNECT headers - server expects userId, not JWT token
        List<StompHeader> headers = new ArrayList<>();
        if (mUserId != null) {
            headers.add(new StompHeader("Authorization", "Bearer " + mUserId));
        }

        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, mWebSocketUrl, handshakeHeaders, okHttpClient);
        mStompClient.withClientHeartbeat(15000).withServerHeartbeat(15000);
        
        // Listen for CONNECTED frame to trigger subscriptions
        // Use delay to ensure STOMP CONNECTED frame is processed
        Disposable connectedDisposable = mStompClient.lifecycle()
                .filter(lifecycleEvent -> lifecycleEvent.getType() == ua.naiksoftware.stomp.dto.LifecycleEvent.Type.OPENED)
                .delay(500, java.util.concurrent.TimeUnit.MILLISECONDS) // Wait for CONNECTED frame
                .subscribe(lifecycleEvent -> {
                    Log.i(TAG, "✅ STOMP CONNECTED - Ready to subscribe");
                    subscribeToTopics(); // Subscribe after CONNECTED
                }, throwable -> {
                    Log.e(TAG, "Error subscribing after connection", throwable);
                });
        mComposite.add(connectedDisposable);
        
        mStompClient.connect(headers);

        // Lắng nghe các sự kiện vòng đời (Connected, Closed, Error)
        Disposable lifecycleDisposable = mStompClient.lifecycle()
                .subscribe(
                        lifecycleEvent -> {
                            switch (lifecycleEvent.getType()) {
                                case OPENED:
                                    Log.i(TAG, "STOMP Connection Opened (WebSocket handshake complete)");
                                    if (mListener != null) mListener.onWebSocketOpened();
                                    // DON'T subscribe here - STOMP CONNECT hasn't completed yet
                                    // Wait for CONNECTED frame in stompClient.connect() callback
                                    break;
                                case CLOSED:
                                    Log.i(TAG, "STOMP Connection Closed");
                                    if (mListener != null) mListener.onWebSocketClosed();
                                    break;
                                case ERROR:
                                    Log.e(TAG, "STOMP Connection Error: ", lifecycleEvent.getException());
                                    if (mListener != null) mListener.onWebSocketError(lifecycleEvent.getException().getMessage());
                                    break;
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "STOMP Lifecycle Throwable!", throwable);
                            if (mListener != null) mListener.onWebSocketError(throwable.getMessage());
                        }
                );
        mComposite.add(lifecycleDisposable);
    }

    /**
     * Subscribe to WebSocket topics.
     * Note: Subscription paths do NOT include user ID in the path.
     * Spring's SimpleBroker automatically routes /user/queue/messages to the correct user session
     * based on the authenticated Principal set during STOMP CONNECT.
     */
    private void subscribeToTopics() {
        if (!isConnected()) {
            Log.e(TAG, "Cannot subscribe: StompClient not connected.");
            return;
        }

        if (mUserId == null || mUserId.isEmpty()) {
            Log.e(TAG, "Cannot subscribe: User ID is null or empty.");
            return;
        }

        // Use constants directly - SimpleBroker handles user-specific routing
        Log.d(TAG, "📡 Subscribing to topics for user: " + mUserId);

        // Kênh 1: Tin nhắn mới (Text và Proposal)
        Disposable topicDisposable = mStompClient.topic(WS_SUB_MESSAGES)
                .subscribe(
                        stompMessage -> {
                            Log.d(TAG, "<<< Received STOMP (Message): " + stompMessage.getPayload());
                            try {
                                Message message = mGson.fromJson(stompMessage.getPayload(), Message.class);
                                if (message != null && mListener != null) {
                                    // Gửi message về cho ChatActivity
                                    mListener.onMessageReceived(message);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing received message JSON", e);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Error on STOMP topic (" + WS_SUB_MESSAGES + ")", throwable);
                            if (mListener != null) mListener.onWebSocketError("Subscription error: " + throwable.getMessage());
                        }
                );
        mComposite.add(topicDisposable);

        // Kênh 2: Cập nhật trạng thái Proposal
        Disposable proposalUpdateDisposable = mStompClient.topic(WS_SUB_PROPOSAL_UPDATES)
                .subscribe(
                        stompMessage -> {
                            Log.d(TAG, "<<< Received STOMP (Proposal Update): " + stompMessage.getPayload());
                            try {
                                ProposalUpdateDTO update = mGson.fromJson(stompMessage.getPayload(), ProposalUpdateDTO.class);
                                if (update != null && mListener != null) {
                                    // Gửi update về cho ChatActivity
                                    mListener.onProposalUpdateReceived(update);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing proposal update JSON", e);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Error on STOMP topic (" + WS_SUB_PROPOSAL_UPDATES + ")", throwable);
                            if (mListener != null) mListener.onWebSocketError("Subscription error: " + throwable.getMessage());
                        }
                );
        mComposite.add(proposalUpdateDisposable);

        // Kênh 3: Cập nhật trạng thái tin nhắn (SENT, DELIVERED, READ)
        Disposable statusDisposable = mStompClient.topic(WS_SUB_STATUS_UPDATES)
                .subscribe(
                        stompMessage -> {
                            Log.d(TAG, "<<< Received STOMP (Status Update): " + stompMessage.getPayload());
                            if (mListener != null) {
                                mListener.onStatusUpdateReceived(stompMessage.getPayload());
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Error on STOMP topic (" + WS_SUB_STATUS_UPDATES + ")", throwable);
                        }
                );
        mComposite.add(statusDisposable);

        // Kênh 4: Typing indicators
        Disposable typingDisposable = mStompClient.topic(WS_SUB_TYPING)
                .subscribe(
                        stompMessage -> {
                            Log.d(TAG, "<<< Received STOMP (Typing): " + stompMessage.getPayload());
                            if (mListener != null) {
                                mListener.onTypingIndicatorReceived(stompMessage.getPayload());
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Error on STOMP topic (" + WS_SUB_TYPING + ")", throwable);
                        }
                );
        mComposite.add(typingDisposable);

        // Kênh 5: Notifications
        Disposable notificationsDisposable = mStompClient.topic(WS_SUB_NOTIFICATIONS)
                .subscribe(
                        stompMessage -> {
                            Log.d(TAG, "<<< Received STOMP (Notification): " + stompMessage.getPayload());
                            if (mListener != null) {
                                mListener.onNotificationReceived(stompMessage.getPayload());
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Error on STOMP topic (" + WS_SUB_NOTIFICATIONS + ")", throwable);
                        }
                );
        mComposite.add(notificationsDisposable);
        
        Log.d(TAG, "✅ All subscriptions created successfully for user: " + mUserId);
    }
    
    /**
     * Subscribe to session messages (for shippers to monitor client messages)
     * Shippers can call this when they start a session to receive all client messages
     */
    public void subscribeToSessionMessages() {
        if (!isConnected()) {
            Log.e(TAG, "Cannot subscribe to session messages: Not connected.");
            return;
        }
        
        if (mUserId == null || mUserId.isEmpty()) {
            Log.e(TAG, "Cannot subscribe to session messages: User ID is null or empty.");
            return;
        }
        
        // Subscribe to shipper's session monitoring topic
        String sessionTopic = "/topic/shipper/" + mUserId + "/session-messages";
        Log.d(TAG, "📡 Subscribing to session messages: " + sessionTopic);
        
        Disposable sessionMessagesDisposable = mStompClient.topic(sessionTopic)
                .subscribe(
                        stompMessage -> {
                            Log.d(TAG, "<<< Received STOMP (Session Message): " + stompMessage.getPayload());
                            try {
                                Message message = mGson.fromJson(stompMessage.getPayload(), Message.class);
                                if (message != null && mListener != null) {
                                    // Notify listener about session message
                                    mListener.onSessionMessageReceived(message);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing session message JSON", e);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Error on STOMP topic (" + sessionTopic + ")", throwable);
                            if (mListener != null) {
                                mListener.onWebSocketError("Session subscription error: " + throwable.getMessage());
                            }
                        }
                );
        mComposite.add(sessionMessagesDisposable);
        
        Log.d(TAG, "✅ Session messages subscription created for shipper: " + mUserId);
    }

    /**
     * Gửi tin nhắn TEXT (logic chat cũ).
     */
    public void sendMessage(ChatMessagePayload payload, final SendMessageCallback callback) {
        if (!isConnected()) {
            Log.e(TAG, "Cannot send message: Not connected.");
            callback.onError(new Exception("Not connected"));
            return;
        }

        String jsonPayload = mGson.toJson(payload);
        Log.d(TAG, ">>> Sending STOMP to " + WS_SEND_MESSAGE);

        Disposable sendDisposable = mStompClient.send(WS_SEND_MESSAGE, jsonPayload)
                .subscribe(
                        () -> { // onSuccess
                            Log.d(TAG, "STOMP message sent successfully.");
                            callback.onSuccess();
                        },
                        throwable -> { // onError
                            Log.e(TAG, "Error sending STOMP message", throwable);
                            callback.onError(throwable);
                        }
                );
        mComposite.add(sendDisposable);
    }

    /**
     * Send typing indicator
     */
    public void sendTypingIndicator(String conversationId, boolean isTyping) {
        if (!isConnected()) {
            Log.e(TAG, "Cannot send typing indicator: Not connected.");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("isTyping", isTyping);
        payload.put("timestamp", System.currentTimeMillis());

        String jsonPayload = mGson.toJson(payload);
        Log.d(TAG, ">>> Sending typing indicator to " + WS_SEND_TYPING);

        Disposable sendDisposable = mStompClient.send(WS_SEND_TYPING, jsonPayload)
                .subscribe(
                        () -> Log.d(TAG, "Typing indicator sent successfully."),
                        throwable -> Log.e(TAG, "Error sending typing indicator", throwable)
                );
        mComposite.add(sendDisposable);
    }

    /**
     * Mark messages as read
     */
    public void markMessagesAsRead(String[] messageIds, String conversationId) {
        if (!isConnected()) {
            Log.e(TAG, "Cannot mark as read: Not connected.");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("messageIds", messageIds);
        payload.put("conversationId", conversationId);

        String jsonPayload = mGson.toJson(payload);
        Log.d(TAG, ">>> Sending read receipt to " + WS_SEND_READ);

        Disposable sendDisposable = mStompClient.send(WS_SEND_READ, jsonPayload)
                .subscribe(
                        () -> Log.d(TAG, "Read receipt sent successfully."),
                        throwable -> Log.e(TAG, "Error sending read receipt", throwable)
                );
        mComposite.add(sendDisposable);
    }

    /**
     * Send quick action for proposal
     */
    public void sendQuickAction(String proposalId, String action, Map<String, Object> data) {
        if (!isConnected()) {
            Log.e(TAG, "Cannot send quick action: Not connected.");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("proposalId", proposalId);
        payload.put("action", action);
        if (data != null) {
            payload.putAll(data);
        }

        String jsonPayload = mGson.toJson(payload);
        Log.d(TAG, ">>> Sending quick action to " + WS_SEND_QUICK_ACTION);

        Disposable sendDisposable = mStompClient.send(WS_SEND_QUICK_ACTION, jsonPayload)
                .subscribe(
                        () -> Log.d(TAG, "Quick action sent successfully."),
                        throwable -> Log.e(TAG, "Error sending quick action", throwable)
                );
        mComposite.add(sendDisposable);
    }

    /**
     * Ngắt kết nối và dọn dẹp.
     */
    public void disconnect() {
        Log.d(TAG, "Disconnecting STOMP and disposing subscriptions.");
        if (mStompClient != null) {
            mStompClient.disconnect();
            mStompClient = null;
        }
        if (mComposite != null && !mComposite.isDisposed()) {
            mComposite.dispose();
            mComposite = null;
        }
    }

    // Interface callback nội bộ cho việc gửi tin
    public interface SendMessageCallback {
        void onSuccess();
        void onError(Throwable throwable);
    }
}
