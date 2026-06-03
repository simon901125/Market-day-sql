package com.example.demo.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新目前登入者基本資料請求")
public class UpdateUserProfileRequest {

    @Schema(description = "使用者姓名", example = "Updated Test User")
    private String name;

    @Schema(description = "使用者電話", example = "+886911111111")
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
