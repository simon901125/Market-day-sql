package com.example.demo.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Google credential 請求資料")
public class GoogleCredentialRequest {

    @Schema(description = "Google Identity Services 回傳的 credential / ID token", example = "GOOGLE_ID_TOKEN")
    private String credential;

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}
