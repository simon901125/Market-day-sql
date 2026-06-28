package com.example.demo.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Service.UserService;
import com.example.demo.dto.EmailVerificationRequest;
import com.example.demo.dto.GoogleCredentialRequest;
import com.example.demo.dto.LocalLoginRequest;
import com.example.demo.dto.LocalRegisterRequest;
import com.example.demo.dto.RequestPasswordResetRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.UpdateUserProfileRequest;
import com.example.demo.entity.Users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Tag(name = "使用者與驗證 API", description = "提供使用者註冊、登入、登出、信箱驗證、密碼重設、目前登入者資料與帳號管理功能。")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "查詢所有使用者", description = "查詢 users 資料表中的所有使用者資料，主要供開發測試或管理端檢視使用。")
    @GetMapping("/usersall")
    public List<Users> show() {
        return userService.findAllUsers();
    }

    @Operation(summary = "攤主本地端註冊", description = "使用 email、密碼與姓名建立 VENDOR 帳號，並寄送信箱驗證碼。")
    @PostMapping("/api/vendor/local-register")
    public String vendorRegister(@Valid @RequestBody LocalRegisterRequest user) {
        return userService.registerLocal(user, "VENDOR");
    }

    @Operation(summary = "主辦方本地端註冊", description = "使用 email、密碼與姓名建立 ORGANIZER 帳號，並寄送信箱驗證碼。")
    @PostMapping("/api/organizer/local-register")
    public String organizerRegister(@Valid @RequestBody LocalRegisterRequest user) {
        return userService.registerLocal(user, "ORGANIZER");
    }

    @Operation(summary = "攤主 Google 註冊", description = "使用 Google credential 建立 VENDOR 帳號，並寄送信箱驗證碼。")
    @PostMapping("/api/vendor/google-register")
    public String vendorRegisterWithGoogle(@Valid @RequestBody GoogleCredentialRequest body) {
        return userService.registerGoogle(body, "VENDOR");
    }

    @Operation(summary = "主辦方 Google 註冊", description = "使用 Google credential 建立 ORGANIZER 帳號，並寄送信箱驗證碼。")
    @PostMapping("/api/organizer/google-register")
    public String organizerRegisterWithGoogle(@Valid @RequestBody GoogleCredentialRequest body) {
        return userService.registerGoogle(body, "ORGANIZER");
    }

    @Operation(summary = "攤主本地端登入", description = "使用 email 與密碼登入 VENDOR 帳號，成功後回傳 JWT token。")
    @PostMapping("/api/vendor/local-login")
    public Map<String, Object> vendorLogin(@Valid @RequestBody LocalLoginRequest body) {
        return userService.loginLocal(body, "VENDOR");
    }

    @Operation(summary = "主辦方本地端登入", description = "使用 email 與密碼登入 ORGANIZER 帳號，成功後回傳 JWT token。")
    @PostMapping("/api/organizer/local-login")
    public Map<String, Object> organizerLogin(@Valid @RequestBody LocalLoginRequest body) {
        return userService.loginLocal(body, "ORGANIZER");
    }

    @Operation(summary = "管理員本地端登入", description = "使用系統建立的管理員 email 與密碼登入 ADMIN 後台。")
    @PostMapping("/api/admin/local-login")
    public Map<String, Object> adminLogin(@Valid @RequestBody LocalLoginRequest body) {
        return userService.loginLocal(body, "ADMIN");
    }

    @Operation(summary = "攤主 Google 登入", description = "使用 Google credential 登入 VENDOR 帳號，成功後回傳 JWT token。")
    @PostMapping("/api/vendor/google-login")
    public Map<String, Object> vendorloginWithGoogle(@Valid @RequestBody GoogleCredentialRequest body) {
        return userService.loginGoogle(body, "VENDOR");
    }

    @Operation(summary = "主辦方 Google 登入", description = "使用 Google credential 登入 ORGANIZER 帳號，成功後回傳 JWT token。")
    @PostMapping("/api/organizer/google-login")
    public Map<String, Object> organizerloginWithGoogle(@Valid @RequestBody GoogleCredentialRequest body) {
        return userService.loginGoogle(body, "ORGANIZER");
    }

    @Operation(summary = "驗證註冊信箱", description = "使用 email 與 6 位數驗證碼完成註冊信箱驗證，驗證成功後啟用帳號。")
    @PostMapping("/api/auth/createAccount/emailVerify")
    public Map<String, Object> verifyCreateAccountEmail(@Valid @RequestBody EmailVerificationRequest body) {
        return userService.verifyCreateAccountEmail(body);
    }

    @Operation(summary = "寄送重設密碼驗證碼", description = "若 email 屬於本地帳號，寄送 6 位數重設密碼驗證碼。")
    @PostMapping("/api/auth/resetPassword/request")
    public Map<String, Object> requestPasswordReset(
            @Valid @RequestBody RequestPasswordResetRequest body) {
        return userService.requestPasswordReset(body);
    }

    @Operation(summary = "驗證重設密碼信箱", description = "使用 email 與 6 位數驗證碼確認重設密碼流程，成功後回傳 resetToken。")
    @PostMapping("/api/auth/resetPassword/emailVerify")
    public Map<String, Object> verifyResetPasswordEmail(@Valid @RequestBody EmailVerificationRequest body) {
        return userService.verifyResetPasswordEmail(body);
    }

    @Operation(summary = "重設密碼", description = "使用一次性的 resetToken 與新密碼重設本地端帳號密碼。")
    @PostMapping("/api/auth/resetPassword/reset")
    public Map<String, Object> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        return userService.resetPassword(body);
    }

    @Operation(summary = "登出", description = "需要 Authorization header 帶入 Bearer JWT；登出後會將目前 token 加入黑名單，並將登入狀態改為未登入。")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/auth/logout")
    public Map<String, Object> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return userService.logout(authorizationHeader);
    }

    @Operation(summary = "取得目前登入者資訊", description = "需要 Authorization header 帶入 Bearer JWT；依 token 中的 email 查詢目前登入者基本資料。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/auth/me")
    public Map<String, Object> me(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return userService.getCurrentUser(authorizationHeader);
    }

    @Operation(summary = "更新目前登入者資料", description = "需要 Authorization header 帶入 Bearer JWT；可更新目前登入者的姓名與電話。")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/users/me")
    public Map<String, Object> updateMe(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody UpdateUserProfileRequest body) {
        return userService.updateCurrentUser(authorizationHeader, body);
    }

    @Operation(summary = "停用目前登入帳號", description = "需要 Authorization header 帶入 Bearer JWT；依帳號角色檢查是否仍有進行中的活動或申請，通過後停用帳號並登出。")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/account/deactivate")
    public Map<String, Object> deactivateAccount(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return userService.deactivateCurrentAccount(authorizationHeader);
    }
}
