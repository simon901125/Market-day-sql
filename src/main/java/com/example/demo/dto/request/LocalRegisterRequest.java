package com.example.demo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "本地端註冊請求")
public class LocalRegisterRequest {

    @Schema(description = "使用者角色", example = "VENDOR/ORGANIZER", allowableValues = {"VENDOR", "ORGANIZER", "ADMIN"})
    private String role;

    @NotBlank(message = "Name is required")
    @Size(max = 20, message = "Name must not exceed 20 characters")
    @Schema(description = "使用者姓名，最多 20 個字元", example = "王小明")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "使用者 Email", example = "simon901125@gmail.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters and contain letters and numbers")
    @Schema(description = "密碼，至少 8 個字元，且需包含英文與數字", example = "Password123")
    private String password;

    @Pattern(regexp = "^$|^09\\d{8}$", message = "Phone must be 10 digits and start with 09")
    @Schema(description = "台灣手機號碼，10 位數字且以 09 開頭", example = "0912345678")
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
