package com.example.demo.Service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.GoogleTokenInfo;
import com.example.demo.entity.Users;
import com.example.demo.dto.EmailVerificationRequest;
import com.example.demo.dto.GoogleCredentialRequest;
import com.example.demo.dto.LocalLoginRequest;
import com.example.demo.dto.LocalRegisterRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.UpdateUserProfileRequest;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailService emailService;

    @Value("${google.client-id}")
    private String googleClientId;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final int AUTO_LOGOUT_TIMEOUT_HOURS = 1;

    public List<Users> findAllUsers() {
        return userRepository.findAllUsers();
    }

    public String registerLocal(LocalRegisterRequest user, String role) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "Email already registered";
        }

        Long userId = userRepository.createLocalUser(
                role,
                user.getName(),
                user.getEmail(),
                authService.hashPassword(user.getPassword()),
                user.getPhone());

        String verificationCode = generateVerificationCode();
        userRepository.deleteEmailVerificationTokensByUserId(userId);
        userRepository.createEmailVerificationToken(userId, verificationCode, LocalDateTime.now().plusMinutes(10));
        emailService.sendVerificationCode(user.getEmail(), verificationCode);
        return "User registered successfully. Verification code has been sent to email";
    }

    public String registerGoogle(GoogleCredentialRequest body, String role) {
        String credential = body.getCredential();
        if (credential == null || credential.isBlank()) {
            return "Google credential is required";
        }

        GoogleTokenInfo tokenInfo = authService.verifyGoogleCredential(credential);
        if (tokenInfo == null) {
            return "Invalid Google credential";
        }

        String validationMessage = validateGoogleTokenInfo(tokenInfo);
        if (validationMessage != null) {
            return validationMessage;
        }

        if (userRepository.existsByEmail(tokenInfo.getEmail())) {
            return "Google account has already register";
        }

        Long userId = userRepository.createGoogleUser(role, tokenInfo.getName(), tokenInfo.getEmail());

        String verificationCode = generateVerificationCode();
        userRepository.deleteEmailVerificationTokensByUserId(userId);
        userRepository.createEmailVerificationToken(userId, verificationCode, LocalDateTime.now().plusMinutes(10));
        emailService.sendVerificationCode(tokenInfo.getEmail(), verificationCode);

        return "Google user registered successfully. Verification code has been sent to email";
    }

    @Transactional
    public Map<String, Object> loginLocal(LocalLoginRequest body, String expectedRole) {
        Optional<Map<String, Object>> userData = userRepository.findLocalUserByEmail(body.getEmail());

        if (userData.isEmpty()
                || !authService.matchesPassword(body.getPassword(), (String) userData.get().get("password_hash"))) {
            return Map.of("message", "Invalid email or password");
        }
        if (userData.get().get("emailVerifiedAt") == null) {
            return Map.of("message", "Email is not verified");
        }
        if (!"ACTIVE".equals(userData.get().get("status"))) {
            return Map.of("message", "Account is not active or isdeleted");
        }
        String role = userData.get().get("role").toString();

        if (!expectedRole.equals(role)) {//如果該登入者的帳號與密碼不符合其身分(ex:用攤主的資訊來登入主辦帳號)
            return Map.of("message", "This account cannot login from this portal");
        }

        if (userRepository.markLoginById(toLong(userData.get().get("id")), AUTO_LOGOUT_TIMEOUT_HOURS) == 0) {
            return Map.of("message", "Login status update failed");
        }
        userData.get().put("isLogin", true);

        return buildLoginResponse(userData.get(), "Login successful");
    }

    @Transactional
    public Map<String, Object> loginGoogle(GoogleCredentialRequest body, String expectedRole) {
        String credential = body.getCredential();
        if (credential == null || credential.isBlank()) {
            return Map.of("message", "Google credential is required");
        }

        GoogleTokenInfo tokenInfo = authService.verifyGoogleCredential(credential);
        if (tokenInfo == null) {
            return Map.of("message", "Invalid Google credential");
        }

        String validationMessage = validateGoogleTokenInfo(tokenInfo);
        if (validationMessage != null) {
            return Map.of("message", validationMessage);
        }
        Optional<Map<String, Object>> userData = userRepository.findProfileByEmail(tokenInfo.getEmail());
        if (userData.isEmpty()) {
            return Map.of("message", "Google account is not registered");
        }
        if (userData.get().get("emailVerifiedAt") == null) {
            return Map.of("message", "Email is not verified");
        }
        if (!"ACTIVE".equals(userData.get().get("status"))) {
            return Map.of("message", "Account is not active");
        }

        //比對身分與登入帳號者
        String role = userData.get().get("role").toString();
        if (!expectedRole.equals(role)) {
            return Map.of("message", "This account cannot login from this portal");
        }

        if (userRepository.markLoginById(toLong(userData.get().get("id")), AUTO_LOGOUT_TIMEOUT_HOURS) == 0) {
            return Map.of("message", "Login status update failed");
        }
        userData.get().put("isLogin", true);

        return buildLoginResponse(userData.get(), "Google login successful");
    }

    public Map<String, Object> logout(String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);
        if (token == null || token.isBlank()) {
            return Map.of("message", "Authorization token is required");
        }
        if (!jwtService.isTokenValid(token)) {
            return Map.of("message", "Invalid or expired token");
        }

        String email = jwtService.getEmail(token);
        userRepository.markLogoutByEmail(email);
        //將token放入失效名單
        jwtService.revokeToken(token);
        return Map.of("message", "Logout successful");
    }

    @Scheduled(fixedRate = 60_000)
    public void autoLogout() {
        userRepository.autoLogoutExpiredUsers();
    }

    public Map<String, Object> getCurrentUser(String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);

        String email = jwtService.getEmail(token);
        return userRepository.findProfileByEmail(email)
                .<Map<String, Object>>map(user -> Map.of("message", "User info retrieved successfully", "user", user))
                .orElseGet(() -> Map.of("message", "User not found"));
    }

    public Map<String, Object> updateCurrentUser(String authorizationHeader, UpdateUserProfileRequest body) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);

        String email = jwtService.getEmail(token);
        //////////////////////////
        int updatedRows = userRepository.updateProfileByEmail(email, body.getName(), body.getPhone());
        if (updatedRows == 0) {
            return Map.of("message", "User not found");
        }

        return userRepository.findProfileByEmail(email)
                .<Map<String, Object>>map(user -> Map.of("message", "User profile updated successfully", "user", user))
                .orElseGet(() -> Map.of("message", "User not found"));
    }

    @Transactional
    public Map<String, Object> deactivateCurrentAccount(String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);
        if (token == null || token.isBlank()) {
            return Map.of("message", "Authorization token is required");
        }
        if (!jwtService.isTokenValid(token)) {
            return Map.of("message", "Invalid or expired token");
        }

        String email = jwtService.getEmail(token);
        String tokenRole = jwtService.getRole(token);
        Map<String, Object> user = userRepository.findProfileByEmail(email)
                .orElse(null);
        if (user == null) {
            return Map.of("message", "User not found");
        }

        String role = user.get("role").toString();
        if (!role.equals(tokenRole)) {
            return Map.of("message", "Account role does not match token role");
        }
        if (!"ACTIVE".equals(user.get("status"))) {
            return Map.of("message", "Account is not active");
        }
        //根據role先判斷是否該帳號還有連結的服務沒有完成
        Long userId = ((Number) user.get("id")).longValue();
        if ("VENDOR".equals(role)) {
            if (userRepository.existsActiveVendorApplication(userId)) {
                return Map.of("message", "Account cannot be deactivated while vendor applications are still in progress");
            }
        } else if ("ORGANIZER".equals(role)) {
            if (userRepository.existsActiveOrganizerEvent(userId)) {
                return Map.of("message", "Account cannot be deactivated while organizer events are still in progress");
            }
        } else {
            return Map.of("message", "This account role cannot be deactivated from this API");
        }

        int updatedRows = userRepository.deactivateUserById(userId);
        if (updatedRows == 0) {
            return Map.of("message", "Account deactivation failed");
        }

        return Map.of("message", "Account deactivated successfully");
    }

    public Map<String, Object> verifyCreateAccountEmail(EmailVerificationRequest body) {
        Map<String, Object> validationError = validateVerificationRequest(body);
        //確認輸入沒有錯誤(email+code)
        if (validationError != null) {
            return validationError;
        }

        Optional<Map<String, Object>> tokenData = findValidVerificationToken(
                body,
                UserRepository.TOKEN_TYPE_EMAIL_VERIFY);
        if (tokenData.isEmpty()) {
            return Map.of("message", "Invalid or expired verification code");
        }

        Map<String, Object> token = tokenData.get();
        if (token.get("email_verified_at") != null) {
            return Map.of("message", "Email already verified");
        }

        Long userId = ((Number) token.get("user_id")).longValue();
        Long tokenId = ((Number) token.get("id")).longValue();
        userRepository.markEmailVerified(userId);
        userRepository.deleteUserToken(tokenId, UserRepository.TOKEN_TYPE_EMAIL_VERIFY);

        return Map.of("message", "Email verified successfully");
    }

    public Map<String, Object> verifyResetPasswordEmail(EmailVerificationRequest body) {
        Map<String, Object> validationError = validateVerificationRequest(body);
        if (validationError != null) {
            return validationError;
        }

        Optional<Map<String, Object>> tokenData = findValidVerificationToken(
                body,
                UserRepository.TOKEN_TYPE_PASSWORD_RESET);
        if (tokenData.isEmpty()) {
            return Map.of("message", "Invalid or expired verification code");
        }

        return Map.of("message", "Password reset email verified successfully");
    }

    public Map<String, Object> resetPassword(ResetPasswordRequest body) {
        if (body.getEmail() == null || body.getEmail().isBlank()) {
            return Map.of("message", "Email is required");
        }
        if (body.getPassword() == null || body.getPassword().isBlank()) {
            return Map.of("message", "Password is required");
        }

        int updatedRows = userRepository.updateLocalPasswordByEmail(
                body.getEmail(),
                authService.hashPassword(body.getPassword()));
        if (updatedRows == 0) {
            return Map.of("message", "Email is not registered as local account");
        }

        return Map.of("message", "Password reset successfully");
    }

    private String generateVerificationCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private Map<String, Object> validateVerificationRequest(EmailVerificationRequest body) {
        if (body.getEmail() == null || body.getEmail().isBlank()) {
            return Map.of("message", "Email is required");
        }
        if (body.getCode() == null || !body.getCode().matches("\\d{6}")) {
            return Map.of("message", "6-digit verification code is required");
        }
        return null;
    }

    private Optional<Map<String, Object>> findValidVerificationToken(
            EmailVerificationRequest body,
            String tokenType) {
        Optional<Map<String, Object>> tokenData = userRepository.findVerificationCode(
                body.getEmail(),
                body.getCode(),
                tokenType);
        if (tokenData.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime expiresAt = toLocalDateTime(tokenData.get().get("expires_at"));
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }

        return tokenData;
    }

    private String validateGoogleTokenInfo(GoogleTokenInfo tokenInfo) {
        if (!googleClientId.equals(tokenInfo.getAud())) {
            return "Google client id does not match";
        }

        if (!Boolean.parseBoolean(tokenInfo.getEmail_verified())) {
            return "Google email is not verified";
        }

        return null;
    }

    private Map<String, Object> buildLoginResponse(Map<String, Object> userData, String message) {
        String userEmail = userData.get("email").toString();
        String role = userData.get("role").toString();
        String token = jwtService.generateToken(userEmail, role);

        Map<String, Object> user = new HashMap<>();
        user.put("email", userEmail);
        user.put("name", userData.get("name"));
        user.put("role", role);
        user.put("status", userData.get("status"));
        user.put("isLogin", userData.get("isLogin"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("token", token);
        response.put("user", user);
        return response;
    }

    private Long toLong(Object value) {
        return ((Number) value).longValue();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }
}
