package com.example.demo.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.demo.entity.GoogleTokenInfo;

@Service
public class AuthService {

    private final RestClient restClient = RestClient.create();

    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean matchesPassword(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
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
}
