package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.req.CreateProposalDTO;
import com.ds.deliveryapp.clients.req.ProposalResponseRequest;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.Conversation;
import com.ds.deliveryapp.clients.res.InteractiveProposal;
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
    Call<BaseResponse<PageResponse<Message>>> getChatHistory(
            @Path("conversationId") String conversationId,
            @Query("userId") String userId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("conversations/find-by-users")
    Call<BaseResponse<Conversation>> getConversationBy2Users(
            @Query("user1") String user1,
            @Query("user2") String user2
    );

    /**
     * Gửi yêu cầu tạo một proposal mới
     * (POST /api/v1/proposals)
     */
    @POST("proposals")
    Call<BaseResponse<InteractiveProposal>> createProposal(
            @Body CreateProposalDTO payload
    );

    @POST("proposals/{proposalId}/respond")
    Call<BaseResponse<InteractiveProposal>> respondToProposal(
            @Path("proposalId") UUID proposalId,
            @Query("userId") String userId,
            @Body ProposalResponseRequest payload
    );

    /**
     * Lấy danh sách các loại proposal mà user hiện tại
     * được phép tạo (dựa trên role).
     * (GET /api/v1/proposals/available-configs)
     */
    @GET("proposals/available-configs")
    Call<BaseResponse<List<ProposalTypeConfig>>> getAvailableConfigs(
            @Query("roles") List<String> roles
    );

    /**
     * Lấy danh sách conversations của user
     * (GET /api/v1/conversations/user/{userId})
     */
    @GET("conversations/user/{userId}")
    Call<BaseResponse<List<Conversation>>> getConversations(
            @Path("userId") String userId,
            @Query("includeMessages") boolean includeMessages,
            @Query("messageLimit") int messageLimit
    );
}
