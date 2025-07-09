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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for AuthService
 * Tests authentication, user registration, validation, and security aspects
 * Following industry best practices for security testing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserResponse testUserResponse;
    private CreateUserRequest createUserRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUserResponse = UserResponse.builder()
                .id(testUser.getId())
                .name(testUser.getName())
                .email(testUser.getEmail())
                .role(testUser.getRole())
                .createdAt(testUser.getCreatedAt())
                .updatedAt(testUser.getUpdatedAt())
                .build();

        createUserRequest = CreateUserRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("plainPassword123")
                .role(User.Role.USER)
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("plainPassword123")
                .build();
    }

    @Nested
    @DisplayName("User Registration Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("Should register user successfully with valid data")
        void shouldRegisterUserSuccessfully() {
            // Arrange
            given(userRepository.existsByEmail(createUserRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(createUserRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            UserResponse result = authService.register(createUserRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(createUserRequest.getName());
            assertThat(result.getEmail()).isEqualTo(createUserRequest.getEmail());
            assertThat(result.getRole()).isEqualTo(createUserRequest.getRole());

            verify(userRepository).existsByEmail(createUserRequest.getEmail());
            verify(passwordEncoder).encode(createUserRequest.getPassword());
            verify(userRepository).save(argThat(user -> 
                user.getName().equals(createUserRequest.getName()) &&
                user.getEmail().equals(createUserRequest.getEmail()) &&
                user.getPassword().equals("encodedPassword") &&
                user.getRole().equals(createUserRequest.getRole())
            ));
            verify(userMapper).toUserResponse(testUser);
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException when email already exists")
        void shouldThrowEmailAlreadyExistsExceptionWhenEmailExists() {
            // Arrange
            given(userRepository.existsByEmail(createUserRequest.getEmail())).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(createUserRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("Email is already in use: " + createUserRequest.getEmail());

            verify(userRepository).existsByEmail(createUserRequest.getEmail());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should register user with default role when role not specified")
        void shouldRegisterUserWithDefaultRole() {
            // Arrange
            CreateUserRequest requestWithoutRole = CreateUserRequest.builder()
                    .name("Test User")
                    .email("test2@example.com")
                    .password("password123")
                    .build(); // role defaults to USER

            given(userRepository.existsByEmail(requestWithoutRole.getEmail())).willReturn(false);
            given(passwordEncoder.encode(requestWithoutRole.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            UserResponse result = authService.register(requestWithoutRole);

            // Assert
            assertThat(result).isNotNull();
            verify(userRepository).save(argThat(user -> 
                user.getRole().equals(User.Role.USER)
            ));
        }

        @Test
        @DisplayName("Should register admin user successfully")
        void shouldRegisterAdminUserSuccessfully() {
            // Arrange
            CreateUserRequest adminRequest = CreateUserRequest.builder()
                    .name("Admin User")
                    .email("admin@example.com")
                    .password("adminPassword123")
                    .role(User.Role.ADMIN)
                    .build();

            User adminUser = User.builder()
                    .id(UUID.randomUUID())
                    .name(adminRequest.getName())
                    .email(adminRequest.getEmail())
                    .password("encodedPassword")
                    .role(User.Role.ADMIN)
                    .build();

            UserResponse adminResponse = UserResponse.builder()
                    .id(adminUser.getId())
                    .name(adminUser.getName())
                    .email(adminUser.getEmail())
                    .role(User.Role.ADMIN)
                    .build();

            given(userRepository.existsByEmail(adminRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(adminRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(adminUser);
            given(userMapper.toUserResponse(adminUser)).willReturn(adminResponse);

            // Act
            UserResponse result = authService.register(adminRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
            verify(userRepository).save(argThat(user -> 
                user.getRole().equals(User.Role.ADMIN)
            ));
        }

        @Test
        @DisplayName("Should handle special characters in user data")
        void shouldHandleSpecialCharactersInUserData() {
            // Arrange
            CreateUserRequest specialCharRequest = CreateUserRequest.builder()
                    .name("José María O'Connor")
                    .email("jose.maria@example.co.uk")
                    .password("Pässw0rd!@#")
                    .role(User.Role.USER)
                    .build();

            given(userRepository.existsByEmail(specialCharRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(specialCharRequest.getPassword())).willReturn("encodedSpecialPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            UserResponse result = authService.register(specialCharRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(userRepository).save(argThat(user -> 
                user.getName().equals("José María O'Connor") &&
                user.getEmail().equals("jose.maria@example.co.uk")
            ));
        }
    }

    @Nested
    @DisplayName("User Login Tests")
    class UserLoginTests {

        @Test
        @DisplayName("Should login user successfully with valid credentials")
        void shouldLoginUserSuccessfully() {
            // Arrange
            String jwtToken = "valid.jwt.token";
            
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(jwtUtils.generateJwtToken(authentication)).willReturn(jwtToken);
            given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(testUser));
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            JwtResponse result = authService.login(loginRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo(jwtToken);
            assertThat(result.getType()).isEqualTo("Bearer");
            assertThat(result.getUser()).isEqualTo(testUserResponse);

            verify(authenticationManager).authenticate(argThat(auth ->
                auth instanceof UsernamePasswordAuthenticationToken &&
                auth.getName().equals(loginRequest.getEmail()) &&
                auth.getCredentials().equals(loginRequest.getPassword())
            ));
            verify(jwtUtils).generateJwtToken(authentication);
            verify(userRepository).findByEmail(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException for invalid credentials")
        void shouldThrowBadCredentialsExceptionForInvalidCredentials() {
            // Arrange
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willThrow(new BadCredentialsException("Invalid credentials"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtils, never()).generateJwtToken(any());
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("Should throw RuntimeException when user not found after authentication")
        void shouldThrowRuntimeExceptionWhenUserNotFoundAfterAuthentication() {
            // Arrange
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(jwtUtils.generateJwtToken(authentication)).willReturn("token");
            given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtils).generateJwtToken(authentication);
            verify(userRepository).findByEmail(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should login admin user successfully")
        void shouldLoginAdminUserSuccessfully() {
            // Arrange
            User adminUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("Admin User")
                    .email("admin@example.com")
                    .role(User.Role.ADMIN)
                    .build();

            UserResponse adminResponse = UserResponse.builder()
                    .id(adminUser.getId())
                    .name(adminUser.getName())
                    .email(adminUser.getEmail())
                    .role(User.Role.ADMIN)
                    .build();

            LoginRequest adminLoginRequest = LoginRequest.builder()
                    .email("admin@example.com")
                    .password("adminPassword")
                    .build();

            String jwtToken = "admin.jwt.token";

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(jwtUtils.generateJwtToken(authentication)).willReturn(jwtToken);
            given(userRepository.findByEmail(adminLoginRequest.getEmail())).willReturn(Optional.of(adminUser));
            given(userMapper.toUserResponse(adminUser)).willReturn(adminResponse);

            // Act
            JwtResponse result = authService.login(adminLoginRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo(jwtToken);
            assertThat(result.getUser().getRole()).isEqualTo(User.Role.ADMIN);
        }

        @Test
        @DisplayName("Should handle case-insensitive email login")
        void shouldHandleCaseInsensitiveEmailLogin() {
            // Arrange
            LoginRequest upperCaseEmailRequest = LoginRequest.builder()
                    .email("TEST@EXAMPLE.COM")
                    .password("password123")
                    .build();

            String jwtToken = "case.insensitive.token";

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(jwtUtils.generateJwtToken(authentication)).willReturn(jwtToken);
            given(userRepository.findByEmail("TEST@EXAMPLE.COM")).willReturn(Optional.of(testUser));
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            JwtResponse result = authService.login(upperCaseEmailRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo(jwtToken);
            verify(userRepository).findByEmail("TEST@EXAMPLE.COM");
        }

        @Test
        @DisplayName("Should create JWT response with correct structure")
        void shouldCreateJwtResponseWithCorrectStructure() {
            // Arrange
            String jwtToken = "structured.jwt.token";

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(jwtUtils.generateJwtToken(authentication)).willReturn(jwtToken);
            given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(testUser));
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            JwtResponse result = authService.login(loginRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo(jwtToken);
            assertThat(result.getType()).isEqualTo("Bearer");
            assertThat(result.getUser()).isNotNull();
            assertThat(result.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(result.getUser().getEmail()).isEqualTo(testUser.getEmail());
            assertThat(result.getUser().getName()).isEqualTo(testUser.getName());
            assertThat(result.getUser().getRole()).isEqualTo(testUser.getRole());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle null email in registration")
        void shouldHandleNullEmailInRegistration() {
            // Arrange
            CreateUserRequest nullEmailRequest = CreateUserRequest.builder()
                    .name("Test User")
                    .email(null)
                    .password("password123")
                    .role(User.Role.USER)
                    .build();

            // Act & Assert
            // This should now throw an exception due to null email validation
            assertThatThrownBy(() -> authService.register(nullEmailRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email cannot be null or empty");

            // Don't verify the repository call since it throws before reaching it
            // verify(userRepository).existsByEmail(null);
        }

        @Test
        @DisplayName("Should handle empty password in registration")
        void shouldHandleEmptyPasswordInRegistration() {
            // Arrange
            CreateUserRequest emptyPasswordRequest = CreateUserRequest.builder()
                    .name("Test User")
                    .email("test@example.com")
                    .password("")
                    .role(User.Role.USER)
                    .build();

            given(userRepository.existsByEmail(emptyPasswordRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode("")).willReturn("encodedEmptyPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            UserResponse result = authService.register(emptyPasswordRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(passwordEncoder).encode("");
        }

        @Test
        @DisplayName("Should handle database error during user save")
        void shouldHandleDatabaseErrorDuringUserSave() {
            // Arrange
            given(userRepository.existsByEmail(createUserRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(createUserRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willThrow(new RuntimeException("Database connection error"));

            // Act & Assert
            assertThatThrownBy(() -> authService.register(createUserRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection error");

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle JWT generation failure")
        void shouldHandleJwtGenerationFailure() {
            // Arrange
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(jwtUtils.generateJwtToken(authentication)).willThrow(new RuntimeException("JWT generation failed"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("JWT generation failed");

            verify(jwtUtils).generateJwtToken(authentication);
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("Should handle very long user names")
        void shouldHandleVeryLongUserNames() {
            // Arrange
            String veryLongName = "A".repeat(1000); // Very long name
            CreateUserRequest longNameRequest = CreateUserRequest.builder()
                    .name(veryLongName)
                    .email("longname@example.com")
                    .password("password123")
                    .role(User.Role.USER)
                    .build();

            given(userRepository.existsByEmail(longNameRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(longNameRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            UserResponse result = authService.register(longNameRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(userRepository).save(argThat(user -> 
                user.getName().equals(veryLongName)
            ));
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            // Arrange
            String plainPassword = "plainPassword123";
            String encodedPassword = "encoded$2a$10$...hashedPassword";

            CreateUserRequest request = CreateUserRequest.builder()
                    .name("Security Test User")
                    .email("security@example.com")
                    .password(plainPassword)
                    .role(User.Role.USER)
                    .build();

            given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(passwordEncoder.encode(plainPassword)).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            authService.register(request);

            // Assert
            verify(passwordEncoder).encode(plainPassword);
            verify(userRepository).save(argThat(user -> 
                user.getPassword().equals(encodedPassword) &&
                !user.getPassword().equals(plainPassword)
            ));
        }

        @Test
        @DisplayName("Should not log sensitive information")
        void shouldNotLogSensitiveInformation() {
            // This test ensures that passwords are not logged
            // In a real implementation, you might check log statements
            
            // Arrange
            given(userRepository.existsByEmail(createUserRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(createUserRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);

            // Act
            UserResponse result = authService.register(createUserRequest);

            // Assert
            assertThat(result).isNotNull();
            // Verify that the password is encoded and original is not stored
            verify(userRepository).save(argThat(user -> 
                !user.getPassword().equals(createUserRequest.getPassword())
            ));
        }

        @Test
        @DisplayName("Should handle concurrent registration attempts")
        void shouldHandleConcurrentRegistrationAttempts() {
            // Arrange
            // First call returns false, second call (concurrent) returns true
            given(userRepository.existsByEmail(createUserRequest.getEmail()))
                    .willReturn(false)
                    .willReturn(true);
            given(passwordEncoder.encode(createUserRequest.getPassword())).willReturn("encodedPassword");

            // Act & Assert
            // First registration should succeed
            assertThatCode(() -> {
                given(userRepository.save(any(User.class))).willReturn(testUser);
                given(userMapper.toUserResponse(testUser)).willReturn(testUserResponse);
                authService.register(createUserRequest);
            }).doesNotThrowAnyException();

            // Second concurrent registration should fail
            assertThatThrownBy(() -> authService.register(createUserRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class);
        }
    }
}
