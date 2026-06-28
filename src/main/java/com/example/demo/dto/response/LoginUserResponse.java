package com.example.demo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登入使用者摘要")
public class LoginUserResponse {

    private String email;
    private Object name;
    private String role;
    private Object status;
    private Object isLogin;

    public LoginUserResponse(String email, Object name, String role, Object status, Object isLogin) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.status = status;
        this.isLogin = isLogin;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Object getName() {
        return name;
    }

    public void setName(Object name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Object getStatus() {
        return status;
    }

    public void setStatus(Object status) {
        this.status = status;
    }

    public Object getIsLogin() {
        return isLogin;
    }

    public void setIsLogin(Object isLogin) {
        this.isLogin = isLogin;
    }
}
