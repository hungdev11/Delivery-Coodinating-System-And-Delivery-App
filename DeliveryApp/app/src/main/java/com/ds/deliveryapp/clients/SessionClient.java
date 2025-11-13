package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.req.ScanParcelRequest;
import com.ds.deliveryapp.clients.req.SessionFailRequest;
import com.ds.deliveryapp.clients.req.TaskFailRequest;
import com.ds.deliveryapp.clients.req.RouteInfo;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.clients.res.ShipperInfo;
import com.ds.deliveryapp.model.DeliveryAssignment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SessionClient {
    /**
     * API Quét-để-thêm-task (Scan-to-add).
     * Ánh xạ tới: SessionController.acceptParcelToSession
     */
    @POST("/api/v1/sessions/drivers/{deliveryManId}/accept-parcel")
    Call<DeliveryAssignment> acceptParcelToSession(
            @Path("deliveryManId") String deliveryManId,
            @Body ScanParcelRequest request
    );

    /**
     * Lấy thông tin chi tiết một phiên.
     * Ánh xạ tới: SessionController.getSessionById
     */
    @GET("/api/v1/sessions/{sessionId}")
    Call<DeliverySession> getSessionById(@Path("sessionId") String sessionId);

    /**
     * Shipper chủ động hoàn thành phiên.
     * Ánh xạ tới: SessionController.completeSession
     */
    @POST("/api/v1/sessions/{sessionId}/complete")
    Call<DeliverySession> completeSession(@Path("sessionId") String sessionId);

    /**
     * Shipper báo cáo sự cố (hủy phiên).
     * Ánh xạ tới: SessionController.failSession
     */
    @POST("/api/v1/sessions/{sessionId}/fail")
    Call<DeliverySession> failSession(
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
    Call<PageResponse<DeliveryAssignment>> getSessionTasks(
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
    Call<PageResponse<DeliveryAssignment>> getTasksBySessionId(
            @Path("sessionId") String sessionId,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Lấy lịch sử task (các phiên đã đóng) với bộ lọc.
     * Ánh xạ tới: DeliveryAssignmentController.getTasksHistory
     */
    @GET("/api/v1/assignments/session/delivery-man/{deliveryManId}/tasks")
    Call<PageResponse<DeliveryAssignment>> getTasks(
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
    @POST("/api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/complete")
    Call<DeliveryAssignment> completeTask(
            @Path("deliveryManId") String deliveryManId,
            @Path("parcelId") String parcelId,
            @Body RouteInfo routeInfo
    );

    /**
     * Shipper báo giao hàng THẤT BẠI.
     * Ánh xạ tới: DeliveryAssignmentController.failTask
     */
    @POST("/api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/fail")
    Call<DeliveryAssignment> failTask(
            @Path("deliveryManId") String deliveryManId,
            @Path("parcelId") String parcelId,
            @Body TaskFailRequest request // Body này chứa reason + routeInfo
    );

    /**
     * Shipper báo khách TỪ CHỐI nhận hàng.
     * Ánh xạ tới: DeliveryAssignmentController.refuseTask
     */
    @POST("/api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/refuse")
    Call<DeliveryAssignment> refuseTask(
            @Path("deliveryManId") String deliveryManId,
            @Path("parcelId") String parcelId,
            @Body TaskFailRequest request // Body này chứa reason + routeInfo
    );

    @GET("/api/v1/assignments/current-shipper/parcels/{parcelId}")
    Call<ShipperInfo> getLastestShipperInfoForParcel( //if ok status but null -> not found
            @Path("parcelId") String parcelId
    );

    /**
     * Tạo phiên ở trạng thái CREATED (chuẩn bị nhận đơn).
     * Ánh xạ tới: SessionController.createSessionPrepared
     */
    @POST("/api/v1/sessions/drivers/{deliveryManId}/prepare")
    Call<DeliverySession> createSessionPrepared(@Path("deliveryManId") String deliveryManId);

    /**
     * Chuyển phiên từ CREATED sang IN_PROGRESS (bắt đầu giao hàng).
     * Ánh xạ tới: SessionController.startSession
     */
    @POST("/api/v1/sessions/{sessionId}/start")
    Call<DeliverySession> startSession(@Path("sessionId") String sessionId);

    /**
     * Lấy phiên đang hoạt động (CREATED hoặc IN_PROGRESS) của shipper.
     * Ánh xạ tới: SessionController.getActiveSession
     */
    @GET("/api/v1/sessions/drivers/{deliveryManId}/active")
    Call<DeliverySession> getActiveSession(@Path("deliveryManId") String deliveryManId);
}
