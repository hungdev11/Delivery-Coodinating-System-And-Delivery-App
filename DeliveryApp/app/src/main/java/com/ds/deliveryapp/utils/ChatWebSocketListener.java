package com.ds.deliveryapp.utils;

import com.ds.deliveryapp.clients.req.ProposalUpdateDTO;
import com.ds.deliveryapp.clients.res.Message;

/**
 * Interface để ChatWebSocketManager báo cáo sự kiện
 * ngược lại cho ChatActivity.
 */
public interface ChatWebSocketListener {
    void onWebSocketOpened();
    void onWebSocketClosed();
    void onWebSocketError(String error);
    void onMessageReceived(Message message);
    void onProposalUpdateReceived(ProposalUpdateDTO update);
    void onStatusUpdateReceived(String statusUpdateJson);
    void onTypingIndicatorReceived(String typingIndicatorJson);
    void onNotificationReceived(String notificationJson);
    
    /**
     * Called when a session message is received (shipper monitoring client messages)
     * This is used by shippers to monitor all client communications in their active session
     */
    void onSessionMessageReceived(Message message);
    
    /**
     * Called when an update notification is received from Communication service
     * Other services (session-service, parcel-service, etc.) publish updates to Kafka
     * Communication service consumes and forwards to clients via WebSocket
     * Clients should refresh data when receiving update notifications
     */
    void onUpdateNotificationReceived(String updateNotificationJson);
}
