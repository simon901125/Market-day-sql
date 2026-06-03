package com.example.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.swagger.GoogleCredentialRequest;
import com.example.demo.swagger.LocalLoginRequest;
import com.example.demo.swagger.LocalRegisterRequest;
import com.example.demo.swagger.UpdateUserProfileRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "使用者與驗證 API", description = "使用者註冊、登入、登出與目前登入者資訊 API")
public class UserController {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Value("${google.client-id}")
    private String googleClientId;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    // 查詢所有 users 資料
    @Operation(summary = "查詢所有使用者")
    @GetMapping("/usersall")
    public List<Map<String, Object>> show() {
        String sql = "SELECT * FROM users";
        Map<String, Object> map = new HashMap<>();
        return namedParameterJdbcTemplate.queryForList(sql, map);
    }

    // 本地端註冊的 API
    @Operation(summary = "本地端註冊", description = "建立 LOCAL 使用者帳號，密碼會在後端 hash 後寫入 password_hash。")
    @PostMapping("/api/auth/register/local")
    public String register(@RequestBody LocalRegisterRequest user) {
        if (authService.isEmailExists(user.getEmail())) {
            return "Email already registered";
        }

        String sql = """
                INSERT INTO users (role, name, email, password_hash, phone, provider, status)
                VALUES (:role, :name, :email, :passwordHash, :phone, :provider, :status)
                """;
        Map<String, Object> map = new HashMap<>();
        map.put("role", user.getRole());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("passwordHash", authService.hashPassword(user.getPassword()));
        map.put("phone", user.getPhone());
        map.put("provider", "LOCAL");
        map.put("status", "ACTIVE");
        namedParameterJdbcTemplate.update(sql, map);
        return "User registered successfully";
    }

    // Google 註冊的 API
    @Operation(summary = "Google 註冊", description = "使用 Google credential 建立 GOOGLE 使用者帳號。")
    @PostMapping("/api/auth/register/google")
    public String registerWithGoogle(@RequestBody GoogleCredentialRequest body) {
        String credential = body.getCredential();

        if (credential == null || credential.isBlank()) {
            return "Google credential is required";
        }

        GoogleTokenInfo tokenInfo = authService.verifyGoogleCredential(credential);
        if (tokenInfo == null) {
            return "Invalid Google credential";
        }

        if (!googleClientId.equals(tokenInfo.getAud())) {
            return "Google client id does not match";
        }

        if (!Boolean.parseBoolean(tokenInfo.getEmail_verified())) {
            return "Google email is not verified";
        }

        if (authService.isEmailExists(tokenInfo.getEmail())) {
            return "Google login successfully";
        }

        String sql = """
                INSERT INTO users (role, name, email, password_hash, phone, provider, status)
                VALUES (:role, :name, :email, :passwordHash, :phone, :provider, :status)
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("role", "VENDOR");
        map.put("name", tokenInfo.getName());
        map.put("email", tokenInfo.getEmail());
        map.put("passwordHash", null);
        map.put("phone", null);
        map.put("provider", "GOOGLE");
        map.put("status", "ACTIVE");

        namedParameterJdbcTemplate.update(sql, map);

        return "Google user registered successfully";
    }

    // 本地端登入的 API
    @Operation(summary = "本地端登入", description = "使用 email/password 登入，成功後回傳 JWT token。")
    @PostMapping("/api/auth/login/local")
    public Map<String, Object> login(@RequestBody LocalLoginRequest body) {
        String email = body.getEmail();
        String password = body.getPassword();

        String sql = "SELECT * FROM users WHERE email = :email AND password_hash = :passwordHash";
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("passwordHash", authService.hashPassword(password));
        List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(sql, map);
        if (list.isEmpty()) {
            return Map.of("message", "Invalid email or password");
        }

        Map<String, Object> userData = list.get(0);
        String userEmail = userData.get("email").toString();
        String role = userData.get("role").toString();
        String token = jwtService.generateToken(userEmail, role);

        Map<String, Object> user = new HashMap<>();
        user.put("email", userEmail);
        user.put("name", userData.get("name"));
        user.put("role", role);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("token", token);
        response.put("user", user);
        return response;
    }

    // Google 登入的 API
    @Operation(summary = "Google 登入", description = "使用 Google credential 登入，成功後回傳 JWT token。")
    @PostMapping("/api/auth/login/google")
    public Map<String, Object> loginWithGoogle(@RequestBody GoogleCredentialRequest body) {
        String credential = body.getCredential();
        if (credential == null || credential.isBlank()) {
            return Map.of("message", "Google credential is required");
        }

        GoogleTokenInfo tokenInfo = authService.verifyGoogleCredential(credential);
        if (tokenInfo == null) {
            return Map.of("message", "Invalid Google credential");
        }

        if (!googleClientId.equals(tokenInfo.getAud())) {
            return Map.of("message", "Google client id does not match");
        }

        if (!Boolean.parseBoolean(tokenInfo.getEmail_verified())) {
            return Map.of("message", "Google email is not verified");
        }

        if (!authService.isEmailExists(tokenInfo.getEmail())) {
            return Map.of("message", "Google account is not registered");
        }

        String token = jwtService.generateToken(tokenInfo.getEmail(), "VENDOR");

        Map<String, Object> user = new HashMap<>();
        user.put("email", tokenInfo.getEmail());
        user.put("name", tokenInfo.getName());
        user.put("role", "VENDOR");
        user.put("status", "ACTIVE");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Google login successful");
        response.put("token", token);
        response.put("user", user);
        return response;
    }

    // 登出的 API
    @Operation(summary = "登出", description = "需要在 Authorization header 帶入 Bearer JWT。")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/auth/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);

        if (token == null || token.isBlank()) {
            return Map.of("message", "Authorization token is required");
        }

        if (!jwtService.isTokenValid(token)) {
            return Map.of("message", "Invalid or expired token");
        }

        return Map.of("message", "Logout successful");
    }

    // 取得目前登入者資訊 API
    @Operation(summary = "取得目前登入者資訊", description = "需要在 Authorization header 帶入 Bearer JWT，回傳目前登入者資料。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/auth/me")
    public Map<String, Object> me(@RequestHeader("Authorization") String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);

        if (token == null || token.isBlank()) {
            return Map.of("message", "Authorization token is required");
        }

        if (!jwtService.isTokenValid(token)) {
            return Map.of("message", "Invalid or expired token");
        }

        String email = jwtService.getEmail(token);

        String sql = """
                SELECT id, role, name, email, phone, provider, status
                FROM users
                WHERE email = :email
                """;
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(sql, map);

        if (list.isEmpty()) {
            return Map.of("message", "User not found");
        }

        return Map.of("message", "User info retrieved successfully", "user", list.get(0));
    }

     
    // 修改目前登入者資料 API
    @Operation(summary = "修改目前登入者資料", description = "需要在 Authorization header 帶入 Bearer JWT，可更新 name、phone。")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/users/me")
    public Map<String, Object> updateMe(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody UpdateUserProfileRequest body) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);

        if (token == null || token.isBlank()) {
            return Map.of("message", "Authorization token is required");
        }

        if (!jwtService.isTokenValid(token)) {
            return Map.of("message", "Invalid or expired token");
        }

        String email = jwtService.getEmail(token);

        //根據輸入的 name 和 phone 更新 users 資料表中對應 email 的使用者資料，並更新 updated_at 為目前時間
        String updateSql = """
                UPDATE users
                SET name = COALESCE(:name, name),
                    phone = COALESCE(:phone, phone),
                    updated_at = SYSDATETIME()
                WHERE email = :email
                """;
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("name", body.getName());
        updateMap.put("phone", body.getPhone());
        updateMap.put("email", email);
        int updatedRows = namedParameterJdbcTemplate.update(updateSql, updateMap);

        if (updatedRows == 0) {
            return Map.of("message", "User not found");
        }

        String selectSql = """
                SELECT id, role, name, email, phone, provider, status
                FROM users
                WHERE email = :email
                """;
        Map<String, Object> selectMap = new HashMap<>();
        selectMap.put("email", email);
        List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(selectSql, selectMap);

        if (list.isEmpty()) {
            return Map.of("message", "User not found");
        }

        return Map.of("message", "User profile updated successfully", "user", list.get(0));
    }
}
