package com.ds.deliveryapp.utils;

import android.util.Log;
import com.ds.deliveryapp.clients.req.ChatMessagePayload;
import com.ds.deliveryapp.clients.req.ProposalUpdateDTO;
import com.ds.deliveryapp.clients.res.Message;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class ChatWebSocketManager {

    private static final String TAG = "ChatWebSocketManager";
    private static final String WS_SUB_MESSAGES = "/user/queue/messages";
    private static final String WS_SUB_PROPOSAL_UPDATES = "/user/queue/proposal-updates";
    private static final String WS_SEND_MESSAGE = "/app/chat.send";

    private StompClient mStompClient;
    private CompositeDisposable mComposite;
    private final Gson mGson = new Gson();
    private final String mWebSocketUrl;
    private final String mJwtToken;
    private ChatWebSocketListener mListener; // Listener (chính là ChatActivity)

    public ChatWebSocketManager(String webSocketUrl, String jwtToken) {
        this.mWebSocketUrl = webSocketUrl;
        this.mJwtToken = jwtToken;
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
        if (mJwtToken == null) {
            Log.e(TAG, "Cannot connect WebSocket: Token is null.");
            if (mListener != null) mListener.onWebSocketError("Token is null");
            return;
        }
        if (isConnected()) {
            Log.w(TAG, "WebSocket connection attempt ignored: Already connected.");
            return;
        }

        Log.d(TAG, "Connecting WebSocket to " + mWebSocketUrl);
        mComposite = new CompositeDisposable();

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("Authorization", "Bearer " + mJwtToken));

        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, mWebSocketUrl);
        mStompClient.withClientHeartbeat(15000).withServerHeartbeat(15000);
        mStompClient.connect(headers);

        // Lắng nghe các sự kiện vòng đời (Connected, Closed, Error)
        Disposable lifecycleDisposable = mStompClient.lifecycle()
                .subscribe(
                        lifecycleEvent -> {
                            switch (lifecycleEvent.getType()) {
                                case OPENED:
                                    Log.i(TAG, "STOMP Connection Opened");
                                    if (mListener != null) mListener.onWebSocketOpened();
                                    subscribeToTopics(); // Tự động đăng ký kênh
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
     * Đăng ký 2 kênh: Tin nhắn mới và Cập nhật proposal.
     */
    private void subscribeToTopics() {
        if (!isConnected()) {
            Log.e(TAG, "Cannot subscribe: StompClient not connected.");
            return;
        }

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
