package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "更新目前登入者資料請求")
public class UpdateUserProfileRequest {

    @Size(max = 20, message = "Name must not exceed 20 characters")
    @Schema(description = "使用者姓名，最多 20 個字元", example = "王小明")
    private String name;

    @Pattern(regexp = "^09\\d{8}$", message = "Phone must be 10 digits and start with 09")
    @Schema(description = "台灣手機號碼，10 位數字且以 09 開頭", example = "0912345678")
    private String phone;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
