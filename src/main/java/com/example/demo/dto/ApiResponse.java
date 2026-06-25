package com.example.demo.dto;

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

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(400, message, null);
    }

    public static ApiResponse<Void> fail(int statusCode, String message) {
        return new ApiResponse<>(statusCode, message, null);
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
            return new ApiResponse<>(statusCodeFromMessage(message), message, dataValue);
        }

        if (legacyResponse instanceof String message) {
            return new ApiResponse<>(statusCodeFromMessage(message), message, null);
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
