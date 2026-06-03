package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuthService {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RestClient restClient = RestClient.create();

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
    //驗證Google憑證，如果有效則返回GoogleTokenInfo對象，否則返回null
    public GoogleTokenInfo verifyGoogleCredential(String credential) {
        try {
            return restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={credential}", credential)
                    .retrieve()
                    .body(GoogleTokenInfo.class);
        } catch (Exception e) {
            return null;
        }
    }
    //檢查電子郵件是否已經存在於數據庫中
    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = :email";
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        Integer count = namedParameterJdbcTemplate.queryForObject(sql, map, Integer.class);
        return count != null && count > 0;
    }
}
