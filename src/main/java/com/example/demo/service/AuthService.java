package com.example.demo.service;

import com.example.demo.domain.dto.request.CreateUserRequest;
import com.example.demo.domain.dto.request.LoginRequest;
import com.example.demo.domain.dto.response.JwtResponse;
import com.example.demo.domain.dto.response.UserResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.EmailAlreadyExistsException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(CreateUserRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return userMapper.toUserResponse(savedUser);
    }

    public JwtResponse login(LoginRequest request) {
        log.info("User login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse userResponse = userMapper.toUserResponse(user);

        log.info("User logged in successfully: {}", user.getId());

        return JwtResponse.builder()
                .token(jwt)
                .type("Bearer")
                .user(userResponse)
                .build();
    }
}
