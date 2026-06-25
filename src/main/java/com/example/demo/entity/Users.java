package com.example.demo.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Users {

    private Long id;
    private String role;
    private String name;
    private String email;
    private String passwordHash;
    private String phone;
    private String provider;
    private String status;
    private Boolean isLogin;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime expiredTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
