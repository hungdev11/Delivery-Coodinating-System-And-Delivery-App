package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.req.CompleteTaskRequest;
import com.ds.deliveryapp.clients.req.ScanParcelRequest;
import com.ds.deliveryapp.clients.req.SessionFailRequest;
import com.ds.deliveryapp.clients.req.TaskFailRequest;
import com.ds.deliveryapp.clients.req.RouteInfo;
import com.ds.deliveryapp.clients.res.BaseResponse;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.res.ShipperInfo;
import com.ds.deliveryapp.clients.res.UploadResult;
import com.ds.deliveryapp.model.DeliveryAssignment;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SessionClient {

    @POST("/api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/complete-with-urls")
    Call<BaseResponse<DeliveryAssignment>> completeTaskWithUrls(
            @Path("deliveryManId") String deliveryManId,
            @Path("parcelId") String parcelId,
            @Body CompleteTaskRequest request
    );

    /**
     * Complete task by assignmentId - more efficient endpoint
     * Prefer this endpoint when assignmentId is available
     */
    @POST("/api/v1/assignments/{assignmentId}/complete")
    Call<BaseResponse<DeliveryAssignment>> completeTaskByAssignmentId(
            @Path("assignmentId") String assignmentId,
            @Body CompleteTaskRequest request
    );

    @GET("/api/v1/sessions/list-must-return-warehouse/{sessionId}")
    Call<BaseResponse<List<DeliveryAssignment>>> listMustReturnToWarehouse(@Path("sessionId") String sessionId);

    /**
     * API Quét-để-thêm-task (Scan-to-add).
     * Ánh xạ tới: SessionController.acceptParcelToSession
     */
    @POST("/api/v1/sessions/drivers/{deliveryManId}/accept-parcel")
    Call<BaseResponse<DeliveryAssignment>> acceptParcelToSession(
            @Path("deliveryManId") String deliveryManId,
            @Body ScanParcelRequest request
    );

    /**
     * Lấy thông tin chi tiết một phiên.
     * Ánh xạ tới: SessionController.getSessionById
     */
    @GET("/api/v1/sessions/{sessionId}")
    Call<BaseResponse<DeliverySession>> getSessionById(@Path("sessionId") String sessionId);

    /**
     * Shipper chủ động hoàn thành phiên.
     * Ánh xạ tới: SessionController.completeSession
     */
    @POST("/api/v1/sessions/{sessionId}/complete")
    Call<BaseResponse<DeliverySession>> completeSession(@Path("sessionId") String sessionId);

    /**
     * Shipper báo cáo sự cố (hủy phiên).
     * Ánh xạ tới: SessionController.failSession
     */
    @POST("/api/v1/sessions/{sessionId}/fail")
    Call<BaseResponse<DeliverySession>> failSession(
            @Path("sessionId") String sessionId,
            @Body SessionFailRequest request
    );

    /**
     * Lấy các task của phiên đang hoạt động (CREATED hoặc IN_PROGRESS) (phân trang).
     * Ánh xạ tới: DeliveryAssignmentController.getDailyTasks
     * @deprecated Use getTasksBySessionId instead
     */
    @Deprecated
    @GET("/api/v1/assignments/session/delivery-man/{deliveryManId}/tasks/today")
    Call<BaseResponse<PageResponse<DeliveryAssignment>>> getSessionTasks(
            @Path("deliveryManId") String driverId,
            @Query("status") List<String> status,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Lấy các task của một session cụ thể theo sessionId (phân trang).
     * Ánh xạ tới: DeliveryAssignmentController.getTasksBySessionId
     */
    @GET("/api/v1/assignments/session/{sessionId}/tasks")
    Call<BaseResponse<PageResponse<DeliveryAssignment>>> getTasksBySessionId(
            @Path("sessionId") String sessionId,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Lấy lịch sử task (các phiên đã đóng) với bộ lọc.
     * Ánh xạ tới: DeliveryAssignmentController.getTasksHistory
     */
    @GET("/api/v1/assignments/session/delivery-man/{deliveryManId}/tasks")
    Call<BaseResponse<PageResponse<DeliveryAssignment>>> getTasks(
            @Path("deliveryManId") String driverId,
            @Query("status") List<String> status,
            @Query("createdAtStart") String createdAtStart,
            @Query("createdAtEnd") String createdAtEnd,
            @Query("completedAtStart") String completedAtStart,
            @Query("completedAtEnd") String completedAtEnd,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Shipper báo giao hàng THÀNH CÔNG.
     * Ánh xạ tới: DeliveryAssignmentController.completeTask
     */
    @Multipart
    @POST("/api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/complete")
    Call<BaseResponse<DeliveryAssignment>> completeTask(
            @Path("deliveryManId") String deliveryManId,
            @Path("parcelId") String parcelId,
            // JSON Object phải được gửi như một Part
            @Part("routeInfo") RequestBody routeInfo,
            // Danh sách file
            @Part List<MultipartBody.Part> files
    );

    /**
     * Shipper báo giao hàng THẤT BẠI.
     * Ánh xạ tới: DeliveryAssignmentController.failTask
     */
    @POST("/api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/fail")
    Call<BaseResponse<DeliveryAssignment>> failTask(
            @Path("deliveryManId") String deliveryManId,
            @Path("parcelId") String parcelId,
            @Body TaskFailRequest request // Body này chứa reason + routeInfo
    );

    /**
     * Shipper báo khách TỪ CHỐI nhận hàng.
     * Ánh xạ tới: DeliveryAssignmentController.refuseTask
     */
    @POST("/api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/refuse")
    Call<BaseResponse<DeliveryAssignment>> refuseTask(
            @Path("deliveryManId") String deliveryManId,
            @Path("parcelId") String parcelId,
            @Body TaskFailRequest request // Body này chứa reason + routeInfo
    );

    @GET("/api/v1/assignments/current-shipper/parcels/{parcelId}")
    Call<BaseResponse<ShipperInfo>> getLastestShipperInfoForParcel(
            @Path("parcelId") String parcelId
    );

    /**
     * Lấy demo-route đã tính sẵn cho session hiện tại (gateway -> session-service -> zone-service)
     */
    @GET("/api/v1/delivery-sessions/{sessionId}/demo-route")
    Call<BaseResponse<com.ds.deliveryapp.clients.res.RoutingResponseDto>> getDemoRouteForSession(
            @Path("sessionId") String sessionId,
            @Query("startLat") Double startLat,
            @Query("startLon") Double startLon
    );

    /**
     * Tạo phiên ở trạng thái CREATED (chuẩn bị nhận đơn).
     * Ánh xạ tới: SessionController.createSessionPrepared
     */
    @POST("/api/v1/sessions/drivers/{deliveryManId}/prepare")
    Call<BaseResponse<DeliverySession>> createSessionPrepared(@Path("deliveryManId") String deliveryManId);

    /**
     * Chuyển phiên từ CREATED sang IN_PROGRESS (bắt đầu giao hàng).
     * Ánh xạ tới: SessionController.startSession
     */
    @POST("/api/v1/sessions/{sessionId}/start")
    Call<BaseResponse<DeliverySession>> startSession(
            @Path("sessionId") String sessionId,
            @Body com.ds.deliveryapp.clients.req.StartSessionRequest request
    );
    
    /**
     * Send location update for tracking
     * POST /api/v1/sessions/{sessionId}/tracking
     */
    @POST("/api/v1/sessions/{sessionId}/tracking")
    Call<BaseResponse<Void>> sendLocationUpdate(
            @Path("sessionId") String sessionId,
            @Body com.ds.deliveryapp.clients.req.LocationUpdateRequest request
    );

    /**
     * Lấy phiên đang hoạt động (CREATED hoặc IN_PROGRESS) của shipper.
     * Ánh xạ tới: SessionController.getActiveSession
     */
    @GET("/api/v1/sessions/drivers/{deliveryManId}/active")
    Call<BaseResponse<DeliverySession>> getActiveSession(@Path("deliveryManId") String deliveryManId);
    
    /**
     * Lấy tất cả sessions của một shipper.
     * Ánh xạ tới: SessionController.getAllSessionsForDeliveryMan
     */
    @GET("/api/v1/sessions/drivers/{deliveryManId}/sessions")
    Call<BaseResponse<java.util.List<DeliverySession>>> getAllSessionsForDeliveryMan(
            @Path("deliveryManId") String deliveryManId,
            @Query("excludeParcelId") String excludeParcelId
    );
    
    /**
     * Transfer a parcel from current shipper to another shipper
     * Only allows transferring ON_ROUTE parcels
     */
    @POST("/api/v1/sessions/drivers/{deliveryManId}/transfer-parcel")
    Call<BaseResponse<DeliveryAssignment>> transferParcel(
            @Path("deliveryManId") String deliveryManId,
            @Body com.ds.deliveryapp.clients.req.TransferParcelRequest request
    );
    
    /**
     * Accept a transferred parcel by scanning source session QR
     */
    @POST("/api/v1/sessions/drivers/{deliveryManId}/accept-transferred-parcel")
    Call<BaseResponse<DeliveryAssignment>> acceptTransferredParcel(
            @Path("deliveryManId") String deliveryManId,
            @Body com.ds.deliveryapp.clients.req.AcceptTransferredParcelRequest request
    );
    
    /**
     * Get delivery proofs by assignment ID
     */
    @GET("/api/v1/delivery-proofs/assignments/{assignmentId}")
    Call<BaseResponse<java.util.List<com.ds.deliveryapp.model.DeliveryProof>>> getProofsByAssignment(
            @Path("assignmentId") String assignmentId
    );
    
    /**
     * Return parcel to warehouse (for FAILED/DELAYED assignments)
     */
    @POST("/api/v1/assignments/{assignmentId}/return-to-warehouse")
    Call<BaseResponse<DeliveryAssignment>> returnToWarehouse(
            @Path("assignmentId") String assignmentId,
            @Body CompleteTaskRequest request
    );
}
