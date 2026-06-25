package com.example.demo.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleTokenInfo {
    private String aud;
    private String email;
    private String email_verified;
    private String name;
}
