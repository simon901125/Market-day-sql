package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Google credential 請求")
public class GoogleCredentialRequest {

    @Schema(description = "Google Identity Services credential / ID token", example = "GOOGLE_ID_TOKEN")
    private String credential;

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}
