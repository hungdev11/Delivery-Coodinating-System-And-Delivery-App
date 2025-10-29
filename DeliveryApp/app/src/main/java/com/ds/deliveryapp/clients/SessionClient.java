package com.ds.deliveryapp.clients;

import com.ds.deliveryapp.clients.req.ScanParcelRequest;
import com.ds.deliveryapp.clients.req.SessionFailRequest;
import com.ds.deliveryapp.clients.req.TaskFailRequest;
import com.ds.deliveryapp.clients.req.RouteInfo;
import com.ds.deliveryapp.clients.res.DeliverySession;
import com.ds.deliveryapp.clients.res.PageResponse;
import com.ds.deliveryapp.model.DeliveryAssignment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface Retrofit Client, ánh xạ chính xác tới các API
 * từ SessionController và DeliveryAssignmentController.
 */
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
     * Lấy các task của phiên đang hoạt động (phân trang).
     * Ánh xạ tới: DeliveryAssignmentController.getDailyTasks
     * (Đường dẫn của bạn đã đúng)
     */
    @GET("/api/v1/assignments/session/delivery-man/{deliveryManId}/tasks/today")
    Call<PageResponse<DeliveryAssignment>> getTasksToday(
            @Path("deliveryManId") String driverId,
            @Query("status") List<String> status,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * Lấy lịch sử task (các phiên đã đóng) với bộ lọc.
     * Ánh xạ tới: DeliveryAssignmentController.getTasksHistory
     * (Đường dẫn của bạn đã đúng)
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
     * (Sửa: POST, Path, Path)
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
     * (Sửa: POST, Path, Path, Body)
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
     * (Sửa: POST, Path, Path, Body)
     */
    @POST("/api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/refuse")
    Call<DeliveryAssignment> refuseTask(
            @Path("deliveryManId") String deliveryManId,
            @Path("parcelId") String parcelId,
            @Body TaskFailRequest request // Body này chứa reason + routeInfo
    );

}
