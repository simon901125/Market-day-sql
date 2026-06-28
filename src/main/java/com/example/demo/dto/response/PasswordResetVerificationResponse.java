package com.example.demo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "重設密碼信箱驗證成功回應")
public class PasswordResetVerificationResponse {

    @Schema(description = "一次性重設密碼 token")
    private String resetToken;

    public PasswordResetVerificationResponse(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }
}
