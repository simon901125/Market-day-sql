package com.example.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Repository.UserRepository;

@Service
public class UpdateActiveTimeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    public boolean isCurrentLoginSession(String token) {
        String email = jwtService.getEmail(token);
        String role = jwtService.getRole(token);
        return userRepository.isCurrentLoginSession(
                email,
                role,
                jwtService.getExpiration(token));
    }
}
