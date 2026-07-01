package com.example.demo.dto.response;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ApiResponse<T> {

    private int statusCode;
    private String message;
    private String messageDetails;
    private T data;

    public ApiResponse(boolean success, String message, T data) {
        this(success ? 200 : 400, message, null, data);
    }

    public ApiResponse(boolean success, String message, String messageDetails, T data) {
        this(success ? 200 : 400, message, messageDetails, data);
    }

    public ApiResponse(int statusCode, String message, T data) {
        this(statusCode, message, null, data);
    }

    public ApiResponse(int statusCode, String message, String messageDetails, T data) {
        this.statusCode = statusCode;
        this.message = message;
        this.messageDetails = messageDetails;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, toChineseSuccessMessage(message), data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, toChineseSuccessMessage(message), null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(400, toChineseErrorMessage(message), null);
    }

    public static <T> ApiResponse<T> fail(int statusCode, String message) {
        return new ApiResponse<>(statusCode, toChineseErrorMessage(message), null);
    }

    public static ApiResponse<Object> fromLegacy(Object legacyResponse, String defaultSuccessMessage) {
        if (legacyResponse instanceof Map<?, ?> responseMap) {
            Object rawMessage = responseMap.get("message");
            String message = rawMessage == null ? defaultSuccessMessage : rawMessage.toString();

            Map<String, Object> data = new LinkedHashMap<>();
            responseMap.forEach((key, value) -> {
                if (!"message".equals(String.valueOf(key))) {
                    data.put(String.valueOf(key), value);
                }
            });

            Object dataValue = data.isEmpty() ? null : data;
            int statusCode = statusCodeFromMessage(message);
            return new ApiResponse<>(
                    statusCode,
                    statusCode >= 200 && statusCode < 300
                            ? toChineseSuccessMessage(message)
                            : toChineseErrorMessage(message),
                    dataValue);
        }

        if (legacyResponse instanceof String message) {
            int statusCode = statusCodeFromMessage(message);
            return new ApiResponse<>(
                    statusCode,
                    statusCode >= 200 && statusCode < 300
                            ? toChineseSuccessMessage(message)
                            : toChineseErrorMessage(message),
                    null);
        }

        return new ApiResponse<>(200, toChineseSuccessMessage(defaultSuccessMessage), legacyResponse);
    }

    private static int statusCodeFromMessage(String message) {
        return isSuccessMessage(message) ? 200 : 400;
    }

    private static boolean isSuccessMessage(String message) {
        String normalized = message == null ? "" : message.toLowerCase();
        if (normalized.contains("successful")
                || normalized.contains("successfully")
                || normalized.contains("retrieved")) {
            return true;
        }

        return !(normalized.contains("required")
                || normalized.contains("invalid")
                || normalized.contains("expired")
                || normalized.contains("not found")
                || normalized.contains("not registered")
                || normalized.contains("not verified")
                || normalized.contains("not active")
                || normalized.contains("cannot")
                || normalized.contains("already")
                || normalized.contains("failed")
                || normalized.contains("does not match")
                || normalized.contains("mismatch"));
    }

    private static String toChineseSuccessMessage(String message) {
        if (message == null || message.isBlank()) {
            return "\u64cd\u4f5c\u6210\u529f";
        }

        return switch (message) {
            case "ok" -> "\u64cd\u4f5c\u6210\u529f";
            case "Users retrieved successfully" -> "\u4f7f\u7528\u8005\u5217\u8868\u53d6\u5f97\u6210\u529f";
            case "User registered successfully. Verification code has been sent to email" -> "\u8a3b\u518a\u6210\u529f\uff0c\u9a57\u8b49\u78bc\u5df2\u5bc4\u9001\u81f3 Email";
            case "Google user registered successfully. Verification code has been sent to email" -> "Google \u8a3b\u518a\u6210\u529f\uff0c\u9a57\u8b49\u78bc\u5df2\u5bc4\u9001\u81f3 Email";
            case "Login successful" -> "\u767b\u5165\u6210\u529f";
            case "Google login successful" -> "Google \u767b\u5165\u6210\u529f";
            case "Logout successful" -> "\u767b\u51fa\u6210\u529f";
            case "User info retrieved successfully" -> "\u4f7f\u7528\u8005\u8cc7\u6599\u53d6\u5f97\u6210\u529f";
            case "User profile updated successfully" -> "\u4f7f\u7528\u8005\u8cc7\u6599\u66f4\u65b0\u6210\u529f";
            case "Account deactivated successfully" -> "\u5e33\u865f\u505c\u7528\u6210\u529f";
            case "Email verified successfully" -> "Email \u9a57\u8b49\u6210\u529f";
            case "If the email belongs to a local account, a verification code has been sent" -> "\u82e5\u6b64 Email \u5c6c\u65bc\u672c\u5730\u5e33\u865f\uff0c\u9a57\u8b49\u78bc\u5df2\u5bc4\u9001";
            case "Password reset email verified successfully" -> "\u91cd\u8a2d\u5bc6\u78bc Email \u9a57\u8b49\u6210\u529f";
            case "Password reset successfully" -> "\u5bc6\u78bc\u91cd\u8a2d\u6210\u529f";
            case "Organizer account retrieved successfully" -> "\u4e3b\u8fa6\u65b9\u5e33\u865f\u8cc7\u6599\u53d6\u5f97\u6210\u529f";
            case "Organizer applications retrieved successfully" -> "\u4e3b\u8fa6\u65b9\u7533\u8acb\u5217\u8868\u53d6\u5f97\u6210\u529f";
            case "Organizer application detail retrieved successfully" -> "\u4e3b\u8fa6\u65b9\u7533\u8acb\u8a73\u60c5\u53d6\u5f97\u6210\u529f";
            case "Stall selection successful" -> "\u6524\u4f4d\u9078\u64c7\u6210\u529f";
            case "Event stalls status retrieved successfully" -> "\u6d3b\u52d5\u6524\u4f4d\u72c0\u614b\u53d6\u5f97\u6210\u529f";
            case "Vendor account retrieved successfully" -> "\u6524\u4e3b\u5e33\u865f\u8cc7\u6599\u53d6\u5f97\u6210\u529f";
            case "Vendor stall map retrieved successfully" -> "\u6524\u4e3b\u9078\u4f4d\u5730\u5716\u53d6\u5f97\u6210\u529f";
            default -> isLikelyEnglish(message) ? "\u64cd\u4f5c\u6210\u529f" : message;
        };
    }

    private static String toChineseErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "\u64cd\u4f5c\u5931\u6557";
        }

        return switch (message) {
            case "Admin accounts must be created by the system" -> "\u7ba1\u7406\u54e1\u5e33\u865f\u5fc5\u9808\u7531\u7cfb\u7d71\u5efa\u7acb";
            case "Admin accounts do not support Google registration" -> "\u7ba1\u7406\u54e1\u5e33\u865f\u4e0d\u652f\u63f4 Google \u8a3b\u518a";
            case "Admin accounts do not support Google login" -> "\u7ba1\u7406\u54e1\u5e33\u865f\u4e0d\u652f\u63f4 Google \u767b\u5165";
            case "Email already registered" -> "\u6b64 Email \u5df2\u88ab\u8a3b\u518a";
            case "Email already verified" -> "\u6b64 Email \u5df2\u5b8c\u6210\u9a57\u8b49";
            case "Email is required" -> "\u8acb\u8f38\u5165 Email";
            case "Email is not verified" -> "Email \u5c1a\u672a\u5b8c\u6210\u9a57\u8b49";
            case "Google account has already register" -> "\u6b64 Google \u5e33\u865f\u5df2\u88ab\u8a3b\u518a";
            case "Google account is not registered" -> "\u6b64 Google \u5e33\u865f\u5c1a\u672a\u8a3b\u518a";
            case "Google credential is required" -> "\u8acb\u63d0\u4f9b Google \u767b\u5165\u6191\u8b49";
            case "Invalid Google credential" -> "Google \u767b\u5165\u6191\u8b49\u7121\u6548";
            case "Google client id does not match" -> "Google client id \u4e0d\u7b26\u5408";
            case "Google email is not verified" -> "Google Email \u5c1a\u672a\u5b8c\u6210\u9a57\u8b49";
            case "Invalid email or password" -> "Email \u6216\u5bc6\u78bc\u932f\u8aa4";
            case "Invalid or expired token" -> "Token \u7121\u6548\u6216\u5df2\u904e\u671f";
            case "Invalid or expired verification code" -> "\u9a57\u8b49\u78bc\u7121\u6548\u6216\u5df2\u904e\u671f";
            case "Invalid or expired reset token" -> "\u91cd\u8a2d\u5bc6\u78bc token \u7121\u6548\u6216\u5df2\u904e\u671f";
            case "Authorization token is required" -> "\u8acb\u63d0\u4f9b Authorization token";
            case "Session expired" -> "\u767b\u5165\u72c0\u614b\u5df2\u904e\u671f\uff0c\u8acb\u91cd\u65b0\u767b\u5165";
            case "User not found" -> "\u627e\u4e0d\u5230\u4f7f\u7528\u8005";
            case "Organizer profile not found" -> "\u627e\u4e0d\u5230\u4e3b\u8fa6\u65b9\u8cc7\u6599";
            case "Vendor profile not found" -> "\u627e\u4e0d\u5230\u6524\u4e3b\u8cc7\u6599";
            case "This account is not an organizer" -> "\u6b64\u5e33\u865f\u4e0d\u662f\u4e3b\u8fa6\u65b9\u5e33\u865f";
            case "This account is not a vendor" -> "\u6b64\u5e33\u865f\u4e0d\u662f\u6524\u4e3b\u5e33\u865f";
            case "Application id is required" -> "\u8acb\u63d0\u4f9b\u7533\u8acb ID";
            case "Application number is required" -> "\u8acb\u63d0\u4f9b\u7533\u8acb\u7de8\u865f";
            case "Application not found" -> "\u627e\u4e0d\u5230\u7533\u8acb\u8cc7\u6599";
            case "Application does not belong to this account" -> "\u6b64\u7533\u8acb\u4e0d\u5c6c\u65bc\u76ee\u524d\u767b\u5165\u5e33\u865f";
            case "Application is not selectable for stall map" -> "\u6b64\u7533\u8acb\u76ee\u524d\u4e0d\u80fd\u67e5\u770b\u9078\u4f4d\u5730\u5716";
            case "Application is not approved, paid, or selectable" -> "\u6b64\u7533\u8acb\u5c1a\u672a\u901a\u904e\u5be9\u6838\u3001\u5c1a\u672a\u4ed8\u6b3e\u6216\u4e0d\u80fd\u9078\u4f4d";
            case "Application binding failed" -> "\u7533\u8acb\u7d81\u5b9a\u6524\u4f4d\u5931\u6557";
            case "Stall number is required" -> "\u8acb\u63d0\u4f9b\u6524\u4f4d\u7de8\u865f";
            case "Stall is not available" -> "\u6b64\u6524\u4f4d\u4e0d\u53ef\u9078\u64c7";
            case "Stall has already been selected" -> "\u6b64\u6524\u4f4d\u5df2\u88ab\u9078\u8d70";
            case "Stall selection failed" -> "\u6524\u4f4d\u9078\u64c7\u5931\u6557";
            case "Account is not active", "Account is not active or disabled" -> "\u5e33\u865f\u5c1a\u672a\u555f\u7528\u6216\u5df2\u505c\u7528";
            case "Account role does not match token role" -> "\u5e33\u865f\u89d2\u8272\u8207 token \u89d2\u8272\u4e0d\u7b26\u5408";
            case "Account cannot be deactivated while vendor applications are still in progress" -> "\u5c1a\u6709\u9032\u884c\u4e2d\u7684\u6524\u4e3b\u5831\u540d\uff0c\u7121\u6cd5\u505c\u7528\u5e33\u865f";
            case "Account cannot be deactivated while organizer events are still in progress" -> "\u5c1a\u6709\u9032\u884c\u4e2d\u7684\u4e3b\u8fa6\u6d3b\u52d5\uff0c\u7121\u6cd5\u505c\u7528\u5e33\u865f";
            case "This account role cannot be deactivated from this API" -> "\u6b64\u5e33\u865f\u89d2\u8272\u4e0d\u80fd\u900f\u904e\u6b64 API \u505c\u7528";
            case "Account deactivation failed" -> "\u5e33\u865f\u505c\u7528\u5931\u6557";
            case "This account cannot login from this portal" -> "\u6b64\u5e33\u865f\u4e0d\u80fd\u5f9e\u6b64\u5165\u53e3\u767b\u5165";
            case "Login status update failed" -> "\u767b\u5165\u72c0\u614b\u66f4\u65b0\u5931\u6557";
            case "Reset token is required" -> "\u8acb\u63d0\u4f9b\u91cd\u8a2d\u5bc6\u78bc token";
            case "Password is required" -> "\u8acb\u8f38\u5165\u5bc6\u78bc";
            case "Password reset failed" -> "\u5bc6\u78bc\u91cd\u8a2d\u5931\u6557";
            case "6-digit verification code is required" -> "\u8acb\u8f38\u5165 6 \u4f4d\u6578\u9a57\u8b49\u78bc";
            default -> isLikelyEnglish(message) ? "\u64cd\u4f5c\u5931\u6557" : message;
        };
    }

    private static boolean isLikelyEnglish(String message) {
        return message.matches(".*[A-Za-z].*");
    }
    @JsonIgnore
    public boolean isSuccessStatus() {
        return statusCode >= 200 && statusCode < 300;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageDetails() {
        return messageDetails;
    }

    public void setMessageDetails(String messageDetails) {
        this.messageDetails = messageDetails;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
