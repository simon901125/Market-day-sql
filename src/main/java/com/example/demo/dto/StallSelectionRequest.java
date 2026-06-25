package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "攤位選擇請求")
public class StallSelectionRequest {

    @NotBlank(message = "Application number is required")
    @Pattern(regexp = "^MD\\d{3}$", message = "Application number must match MD plus 3 digits, e.g. MD001")
    @Schema(description = "報名編號，格式為 MD 加 3 位數字", example = "MD001")
    private String applicationNo;

    @NotBlank(message = "Stall number is required")
    @Pattern(regexp = "^[A-Z]\\d{2}$", message = "Stall number must match an uppercase letter plus 2 digits, e.g. A01")
    @Schema(description = "攤位編號，格式為大寫英文字母加 2 位數字", example = "A01")
    private String stallNo;

    public String getApplicationNo() {
        return applicationNo;
    }

    public void setApplicationNo(String applicationNo) {
        this.applicationNo = applicationNo;
    }

    public String getStallNo() {
        return stallNo;
    }

    public void setStallNo(String stallNo) {
        this.stallNo = stallNo;
    }
}
