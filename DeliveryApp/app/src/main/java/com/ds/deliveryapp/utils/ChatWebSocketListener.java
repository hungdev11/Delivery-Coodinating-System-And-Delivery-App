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
}
