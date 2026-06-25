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

    public boolean refreshActiveTimeByToken(String token) {
        String email = jwtService.getEmail(token);
        return userRepository.updateExpiredTimeByEmail(email, UserService.AUTO_LOGOUT_TIMEOUT_HOURS) > 0;
    }
}
