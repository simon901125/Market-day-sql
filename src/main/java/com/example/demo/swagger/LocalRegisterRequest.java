package com.example.demo.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "本地端註冊請求資料")
public class LocalRegisterRequest {

    @Schema(description = "使用者角色", example = "VENDOR", allowableValues = {"VENDOR", "ORGANIZER", "ADMIN"})
    private String role;

    @Schema(description = "使用者姓名", example = "Local Test User")
    private String name;

    @Schema(description = "使用者 Email", example = "local-test@example.test")
    private String email;

    @Schema(description = "使用者密碼", example = "LocalTest123")
    private String password;

    @Schema(description = "使用者電話", example = "+886900000000")
    private String phone;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
