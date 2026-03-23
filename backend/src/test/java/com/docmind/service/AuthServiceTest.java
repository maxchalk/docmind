package com.docmind.service;

import com.docmind.dto.AuthResponse;
import com.docmind.model.Role;
import com.docmind.model.User;
import com.docmind.repository.UserRepository;
import com.docmind.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashed_password")
                .role(Role.EMPLOYEE)
                .build();
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.register("test@example.com", "password123", "EMPLOYEE");

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
        assertEquals("EMPLOYEE", response.getRole());
    }

    @Test
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register("test@example.com", "password123", "EMPLOYEE"));

        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    void login_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.login("test@example.com", "password123");

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void login_wrongPassword_throwsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login("test@example.com", "wrong_password"));

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void login_userNotFound_throwsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login("unknown@example.com", "password123"));

        assertEquals("Invalid credentials", exception.getMessage());
    }
}
