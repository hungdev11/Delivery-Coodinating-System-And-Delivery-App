package com.ds.deliveryapp.utils;

import com.ds.deliveryapp.enums.ParcelStatus;

/**
 * Utility class to map status values to Vietnamese display text
 */
public class StatusMapper {
    
    /**
     * Map ParcelStatus enum to Vietnamese text
     */
    public static String mapParcelStatus(ParcelStatus status) {
        if (status == null) return "N/A";
        
        switch (status) {
            case IN_WAREHOUSE:
                return "Chờ giao";
            case ON_ROUTE:
                return "Đang giao";
            case DELIVERED:
                return "Đã giao";
            case SUCCEEDED:
                return "Thành công";
            case FAILED:
                return "Thất bại";
            case DELAYED:
                return "Trễ";
            case DISPUTE:
                return "Tranh chấp";
            case LOST:
                return "Mất";
            default:
                return status.name();
        }
    }
    
    /**
     * Map ParcelStatus string to Vietnamese text
     */
    public static String mapParcelStatus(String status) {
        if (status == null || status.isEmpty()) return "N/A";
        
        try {
            ParcelStatus parcelStatus = ParcelStatus.valueOf(status.toUpperCase());
            return mapParcelStatus(parcelStatus);
        } catch (IllegalArgumentException e) {
            // If not a valid ParcelStatus, try to map as assignment status
            return mapAssignmentStatus(status);
        }
    }
    
    /**
     * Map assignment status (IN_PROGRESS, COMPLETED, FAILED) to Vietnamese text
     */
    public static String mapAssignmentStatus(String status) {
        if (status == null || status.isEmpty()) return "MỚI";
        
        switch (status.toUpperCase()) {
            case "IN_PROGRESS":
                return "Đang xử lý";
            case "COMPLETED":
                return "Đã hoàn thành";
            case "FAILED":
                return "Thất bại";
            case "PENDING":
                return "Chờ xử lý";
            case "CREATED":
                return "Mới tạo";
            default:
                return status;
        }
    }
    
    /**
     * Map session status to Vietnamese text
     */
    public static String mapSessionStatus(String status) {
        if (status == null || status.isEmpty()) return "N/A";
        
        switch (status.toUpperCase()) {
            case "CREATED":
                return "Đã tạo";
            case "IN_PROGRESS":
                return "Đang thực hiện";
            case "COMPLETED":
                return "Hoàn thành";
            case "FAILED":
                return "Thất bại";
            default:
                return status;
        }
    }
}
