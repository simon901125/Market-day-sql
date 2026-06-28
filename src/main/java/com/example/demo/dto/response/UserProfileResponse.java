package com.example.demo.dto.response;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "使用者個人資料")
public class UserProfileResponse {

    private Map<String, Object> user;

    public UserProfileResponse(Map<String, Object> user) {
        this.user = user;
    }

    public Map<String, Object> getUser() {
        return user;
    }

    public void setUser(Map<String, Object> user) {
        this.user = user;
    }
}
