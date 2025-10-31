package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.req.CreateProposalDTO; // <-- IMPORT MỚI
import com.ds.deliveryapp.clients.req.ProposalResponseRequest;
import com.ds.deliveryapp.clients.res.Conversation;
import com.ds.deliveryapp.clients.res.InteractiveProposal; // <-- IMPORT MỚI
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.res.ProposalTypeConfig;

import java.util.List;
import java.util.UUID; // <-- IMPORT MỚI
import retrofit2.Call;
import retrofit2.http.Body; // <-- IMPORT MỚI
import retrofit2.http.GET;
import retrofit2.http.Header; // <-- IMPORT MỚI
import retrofit2.http.POST; // <-- IMPORT MỚI
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

    /**
     * Gửi yêu cầu tạo một proposal mới
     * (POST /api/v1/proposals)
     */
    @POST("proposals") // Giả sử Retrofit Base URL đã có /api/v1
    Call<InteractiveProposal> createProposal(
            @Body CreateProposalDTO payload
    );

    @POST("proposals/{proposalId}/respond")
    Call<InteractiveProposal> respondToProposal(
            @Path("proposalId") UUID proposalId,
            @Query("userId") String userId,
            @Body ProposalResponseRequest payload // Gửi { "resultData": "..." }
    );

    /**
     * Lấy danh sách các loại proposal mà user hiện tại
     * được phép tạo (dựa trên role).
     * (GET /api/v1/proposals/available-configs)
     */
    @GET("proposals/available-configs")
    Call<List<ProposalTypeConfig>> getAvailableConfigs(
            @Query("roles") List<String> roles
    );
}