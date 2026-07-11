package com.example.demo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登入成功回應")
public class LoginResponse {

    @Schema(description = "JWT token")
    private String token;

    @Schema(description = "登入使用者資料")
    private LoginUserResponse user;

    public LoginResponse(String token, LoginUserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LoginUserResponse getUser() {
        return user;
    }

    public void setUser(LoginUserResponse user) {
        this.user = user;
    }
}
