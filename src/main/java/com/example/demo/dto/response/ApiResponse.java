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
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
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
                    statusCode >= 200 && statusCode < 300 ? message : toChineseErrorMessage(message),
                    dataValue);
        }

        if (legacyResponse instanceof String message) {
            int statusCode = statusCodeFromMessage(message);
            return new ApiResponse<>(
                    statusCode,
                    statusCode >= 200 && statusCode < 300 ? message : toChineseErrorMessage(message),
                    null);
        }

        return new ApiResponse<>(200, defaultSuccessMessage, legacyResponse);
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

    private static String toChineseErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "操作失敗";
        }

        return switch (message) {
            case "Admin accounts must be created by the system" -> "管理員帳號只能由系統建立";
            case "Admin accounts do not support Google registration" -> "管理員帳號不支援 Google 註冊";
            case "Admin accounts do not support Google login" -> "管理員帳號不支援 Google 登入";
            case "Email already registered" -> "此 Email 已被註冊";
            case "Email already verified" -> "此 Email 已完成驗證";
            case "Email is required" -> "Email 為必填";
            case "Email is not verified" -> "Email 尚未完成驗證";
            case "Google account has already register" -> "此 Google 帳號已被註冊";
            case "Google account is not registered" -> "此 Google 帳號尚未註冊";
            case "Google credential is required" -> "Google 憑證為必填";
            case "Invalid Google credential" -> "Google 憑證無效";
            case "Google client id does not match" -> "Google client id 不符合";
            case "Google email is not verified" -> "Google Email 尚未完成驗證";
            case "Invalid email or password" -> "Email 或密碼錯誤";
            case "Invalid or expired token" -> "Token 無效或已過期";
            case "Invalid or expired verification code" -> "驗證碼無效或已過期";
            case "Invalid or expired reset token" -> "重設密碼 token 無效或已過期";
            case "Authorization token is required" -> "請提供授權 token";
            case "Session expired" -> "登入狀態已過期，請重新登入";
            case "User not found" -> "找不到使用者";
            case "Organizer profile not found" -> "找不到主辦方資料";
            case "Vendor profile not found" -> "找不到攤主資料";
            case "This account is not an organizer" -> "此帳號不是主辦方帳號";
            case "This account is not a vendor" -> "此帳號不是攤主帳號";
            case "Application id is required" -> "申請 ID 為必填";
            case "Application number is required" -> "申請編號為必填";
            case "Application not found" -> "找不到申請資料";
            case "Application does not belong to this account" -> "此申請單不屬於目前登入帳號";
            case "Application is not selectable for stall map" -> "此申請單目前不可查看攤位地圖";
            case "Application is not approved, paid, or selectable" -> "申請尚未審核通過、尚未付款或不可選擇攤位";
            case "Application binding failed" -> "申請綁定攤位失敗";
            case "Stall number is required" -> "攤位編號為必填";
            case "Stall is not available" -> "攤位不可選擇";
            case "Stall has already been selected" -> "搶位失敗，該位置已被選擇";
            case "Stall selection failed" -> "攤位選擇失敗";
            case "Account is not active", "Account is not active or isdeleted" -> "帳號未啟用或已停用";
            case "Account role does not match token role" -> "帳號角色與 token 角色不一致";
            case "Account cannot be deactivated while vendor applications are still in progress" -> "仍有進行中的攤主申請，無法停用帳號";
            case "Account cannot be deactivated while organizer events are still in progress" -> "仍有進行中的主辦活動，無法停用帳號";
            case "This account role cannot be deactivated from this API" -> "此帳號角色無法透過此 API 停用";
            case "Account deactivation failed" -> "帳號停用失敗";
            case "This account cannot login from this portal" -> "此帳號不能從此入口登入";
            case "Login status update failed" -> "登入狀態更新失敗";
            case "Reset token is required" -> "重設密碼 token 為必填";
            case "Password is required" -> "密碼為必填";
            case "Password reset failed" -> "密碼重設失敗";
            case "6-digit verification code is required" -> "請輸入 6 位數驗證碼";
            default -> message;
        };
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
