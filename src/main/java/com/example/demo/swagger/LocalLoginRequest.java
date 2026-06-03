package com.example.demo.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "本地端登入請求資料")
public class LocalLoginRequest {

    @Schema(description = "使用者 Email", example = "local-test@example.test")
    private String email;

    @Schema(description = "使用者密碼", example = "LocalTest123")
    private String password;

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
}
