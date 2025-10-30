package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.res.Conversation;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.clients.res.PageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChatClient {
    @GET("conversations/{conversationId}/messages")
    Call<PageResponse<Message>> getChatHistory(
            @Path("conversationId") String conversationId,
            @Query("userId") String userId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("conversations/find-by-users")
    Call<Conversation> getConversationBy2Users(
            @Query("user1") String user1,
            @Query("user2") String user2
    );
}
