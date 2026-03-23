package com.docmind.service;

import com.docmind.dto.AuthResponse;
import com.docmind.model.Role;
import com.docmind.model.User;
import com.docmind.repository.UserRepository;
import com.docmind.security.JwtUtil;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(String email, String password, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        Role userRole;
        try {
            userRole = Role.valueOf(role.toUpperCase());
        } catch (Exception e) {
            userRole = Role.EMPLOYEE;
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(userRole)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(email);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(email);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
