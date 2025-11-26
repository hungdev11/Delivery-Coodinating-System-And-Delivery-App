package com.ds.deliveryapp.utils;

import android.content.Context;
import android.widget.Toast;

import com.ds.deliveryapp.clients.res.BaseResponse;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;

/**
 * Utility class for handling API responses and showing user-friendly error messages
 * 
 * Usage:
 * <pre>
 * // Automatic handling with Toast
 * BaseResponse&lt;SessionDto&gt; response = ...;
 * SessionDto session = ResponseHandler.handleResponse(context, response);
 * 
 * // Automatic handling with Snackbar
 * SessionDto session = ResponseHandler.handleResponse(view, response);
 * 
 * // Manual toast
 * ResponseHandler.showError(context, "Lỗi khi tải dữ liệu");
 * ResponseHandler.showSuccess(context, "Cập nhật thành công");
 * </pre>
 */
public class ResponseHandler {

    /**
     * Handle BaseResponse and show Toast if error
     * Returns the result data if successful, null if error
     */
    public static <T> T handleResponse(Context context, BaseResponse<T> response) {
        if (response == null) {
            showError(context, "Không nhận được phản hồi từ server");
            return null;
        }

        if (response.getResult() != null) {
            // Success - return the result
            return response.getResult();
        } else {
            // Error - show toast and return null
            String errorMessage = response.getMessage() != null && !response.getMessage().isEmpty()
                    ? response.getMessage()
                    : "Đã xảy ra lỗi";
            showError(context, errorMessage);
            return null;
        }
    }

    /**
     * Handle BaseResponse and show Snackbar if error
     * Returns the result data if successful, null if error
     */
    public static <T> T handleResponse(View view, BaseResponse<T> response) {
        if (response == null) {
            showError(view, "Không nhận được phản hồi từ server");
            return null;
        }

        if (response.getResult() != null) {
            // Success - return the result
            return response.getResult();
        } else {
            // Error - show snackbar and return null
            String errorMessage = response.getMessage() != null && !response.getMessage().isEmpty()
                    ? response.getMessage()
                    : "Đã xảy ra lỗi";
            showError(view, errorMessage);
            return null;
        }
    }

    /**
     * Check if BaseResponse indicates an error
     */
    public static <T> boolean isError(BaseResponse<T> response) {
        return response == null || response.getResult() == null;
    }

    /**
     * Check if BaseResponse indicates success
     */
    public static <T> boolean isSuccess(BaseResponse<T> response) {
        return response != null && response.getResult() != null;
    }

    /**
     * Get error message from BaseResponse
     */
    public static <T> String getErrorMessage(BaseResponse<T> response) {
        if (response == null) {
            return "Không nhận được phản hồi từ server";
        }
        return response.getMessage() != null && !response.getMessage().isEmpty()
                ? response.getMessage()
                : "Đã xảy ra lỗi";
    }

    /**
     * Show error Toast
     */
    public static void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Show error Snackbar
     */
    public static void showError(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction("Đóng", v -> {})
                .show();
    }

    /**
     * Show success Toast
     */
    public static void showSuccess(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show success Snackbar
     */
    public static void showSuccess(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Show info Toast
     */
    public static void showInfo(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show info Snackbar
     */
    public static void showInfo(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }
}
