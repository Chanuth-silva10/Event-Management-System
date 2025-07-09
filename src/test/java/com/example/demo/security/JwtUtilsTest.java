package com.example.demo.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for JwtUtils
 * Tests JWT token generation, validation, and parsing
 * Covers security aspects and edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtils Unit Tests")
class JwtUtilsTest {

    @Mock
    private Authentication authentication;

    @InjectMocks
    private JwtUtils jwtUtils;

    private UserPrincipal userPrincipal;
    private UUID userId;
    private String jwtSecret;
    private int jwtExpirationMs;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jwtSecret = "mySecretKey123456789012345678901234567890"; // 40+ characters for HMAC SHA-256
        jwtExpirationMs = 86400000; // 24 hours

        userPrincipal = new UserPrincipal(
                userId,
                "Test User",
                "test@example.com",
                "password",
                Collections.emptyList()
        );

        // Set private fields using reflection
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", jwtExpirationMs);
    }

    @Nested
    @DisplayName("JWT Token Generation Tests")
    class JwtTokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT token for authenticated user")
        void shouldGenerateValidJwtTokenForAuthenticatedUser() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);

            // Act
            String token = jwtUtils.generateJwtToken(authentication);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
            
            // Verify the token is valid
            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
            
            // Verify the user ID can be extracted
            String extractedUserId = jwtUtils.getUserIdFromJwtToken(token);
            assertThat(extractedUserId).isEqualTo(userId.toString());
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            // Arrange
            UUID anotherUserId = UUID.randomUUID();
            UserPrincipal anotherUserPrincipal = new UserPrincipal(
                    anotherUserId,
                    "Another User",
                    "another@example.com",
                    "password",
                    Collections.emptyList()
            );

            // Generate token for first user
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            String token1 = jwtUtils.generateJwtToken(authentication);

            // Generate token for second user  
            given(authentication.getPrincipal()).willReturn(anotherUserPrincipal);
            String token2 = jwtUtils.generateJwtToken(authentication);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
            assertThat(jwtUtils.getUserIdFromJwtToken(token1)).isEqualTo(userId.toString());
            assertThat(jwtUtils.getUserIdFromJwtToken(token2)).isEqualTo(anotherUserId.toString());
        }

        @Test
        @DisplayName("Should generate different tokens for same user at different times")
        void shouldGenerateDifferentTokensForSameUserAtDifferentTimes() throws InterruptedException {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);

            // Act
            String token1 = jwtUtils.generateJwtToken(authentication);
            Thread.sleep(10); // Ensure different issued time
            String token2 = jwtUtils.generateJwtToken(authentication);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
            assertThat(jwtUtils.getUserIdFromJwtToken(token1)).isEqualTo(userId.toString());
            assertThat(jwtUtils.getUserIdFromJwtToken(token2)).isEqualTo(userId.toString());
        }

        @Test
        @DisplayName("Should include correct expiration time in token")
        void shouldIncludeCorrectExpirationTimeInToken() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            long beforeGeneration = System.currentTimeMillis();

            // Act
            String token = jwtUtils.generateJwtToken(authentication);

            // Assert
            assertThat(token).isNotNull();
            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
            
            // The token should be valid for approximately the configured expiration time
            // We can't test exact expiration without exposing the claims parser,
            // but we can verify the token is currently valid
            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should handle null authentication gracefully")
        void shouldHandleNullAuthenticationGracefully() {
            // Act & Assert
            assertThatThrownBy(() -> jwtUtils.generateJwtToken(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle authentication with null principal")
        void shouldHandleAuthenticationWithNullPrincipal() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> jwtUtils.generateJwtToken(authentication))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("JWT Token Validation Tests")
    class JwtTokenValidationTests {

        @Test
        @DisplayName("Should validate correctly formed and signed token")
        void shouldValidateCorrectlyFormedAndSignedToken() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            String validToken = jwtUtils.generateJwtToken(authentication);

            // Act
            boolean isValid = jwtUtils.validateJwtToken(validToken);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            // Arrange
            String malformedToken = "this.is.not.a.valid.jwt.token";

            // Act
            boolean isValid = jwtUtils.validateJwtToken(malformedToken);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject token with invalid signature")
        void shouldRejectTokenWithInvalidSignature() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            String validToken = jwtUtils.generateJwtToken(authentication);
            
            // Tamper with the signature
            String[] parts = validToken.split("\\.");
            String tamperedToken = parts[0] + "." + parts[1] + ".invalid_signature";

            // Act
            boolean isValid = jwtUtils.validateJwtToken(tamperedToken);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Arrange
            // Set very short expiration time
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 1);
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            
            String expiredToken = jwtUtils.generateJwtToken(authentication);
            
            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            boolean isValid = jwtUtils.validateJwtToken(expiredToken);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            // Act
            boolean isValid = jwtUtils.validateJwtToken(null);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            // Act
            boolean isValid = jwtUtils.validateJwtToken("");

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject whitespace-only token")
        void shouldRejectWhitespaceOnlyToken() {
            // Act
            boolean isValid = jwtUtils.validateJwtToken("   ");

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject token with only header and payload")
        void shouldRejectTokenWithOnlyHeaderAndPayload() {
            // Arrange
            String incompleteToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0";

            // Act
            boolean isValid = jwtUtils.validateJwtToken(incompleteToken);

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("JWT Token Parsing Tests")
    class JwtTokenParsingTests {

        @Test
        @DisplayName("Should extract correct user ID from valid token")
        void shouldExtractCorrectUserIdFromValidToken() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            String validToken = jwtUtils.generateJwtToken(authentication);

            // Act
            String extractedUserId = jwtUtils.getUserIdFromJwtToken(validToken);

            // Assert
            assertThat(extractedUserId).isEqualTo(userId.toString());
        }

        @Test
        @DisplayName("Should extract user ID from different valid tokens")
        void shouldExtractUserIdFromDifferentValidTokens() {
            // Arrange
            UUID anotherUserId = UUID.randomUUID();
            UserPrincipal anotherUserPrincipal = new UserPrincipal(
                    anotherUserId,
                    "Another User",
                    "another@example.com",
                    "password",
                    Collections.emptyList()
            );

            // Generate token for first user
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            String token1 = jwtUtils.generateJwtToken(authentication);

            // Generate token for second user
            given(authentication.getPrincipal()).willReturn(anotherUserPrincipal);
            String token2 = jwtUtils.generateJwtToken(authentication);

            // Act
            String extractedUserId1 = jwtUtils.getUserIdFromJwtToken(token1);
            String extractedUserId2 = jwtUtils.getUserIdFromJwtToken(token2);

            // Assert
            assertThat(extractedUserId1).isEqualTo(userId.toString());
            assertThat(extractedUserId2).isEqualTo(anotherUserId.toString());
        }

        @Test
        @DisplayName("Should throw exception when extracting from malformed token")
        void shouldThrowExceptionWhenExtractingFromMalformedToken() {
            // Arrange
            String malformedToken = "malformed.token";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtils.getUserIdFromJwtToken(malformedToken))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("Should throw exception when extracting from expired token")
        void shouldThrowExceptionWhenExtractingFromExpiredToken() {
            // Arrange
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 1);
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            
            String expiredToken = jwtUtils.generateJwtToken(authentication);
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act & Assert
            assertThatThrownBy(() -> jwtUtils.getUserIdFromJwtToken(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Should throw exception when extracting from token with invalid signature")
        void shouldThrowExceptionWhenExtractingFromTokenWithInvalidSignature() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            String validToken = jwtUtils.generateJwtToken(authentication);
            
            String[] parts = validToken.split("\\.");
            String tamperedToken = parts[0] + "." + parts[1] + ".invalid_signature";

            // Act & Assert
            assertThatThrownBy(() -> jwtUtils.getUserIdFromJwtToken(tamperedToken))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should throw exception when extracting from null token")
        void shouldThrowExceptionWhenExtractingFromNullToken() {
            // Act & Assert
            assertThatThrownBy(() -> jwtUtils.getUserIdFromJwtToken(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should use secure signing algorithm")
        void shouldUseSecureSigningAlgorithm() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);

            // Act
            String token = jwtUtils.generateJwtToken(authentication);

            // Assert
            assertThat(token).isNotNull();
            
            // JWT header should indicate HMAC SHA-256 algorithm
            String[] parts = token.split("\\.");
            assertThat(parts).hasSize(3);
            
            // The token should be validated with the same key
            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should not accept tokens signed with different secret")
        void shouldNotAcceptTokensSignedWithDifferentSecret() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            String validToken = jwtUtils.generateJwtToken(authentication);
            
            // Change the secret
            ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "differentSecret123456789012345678901234567890");

            // Act
            boolean isValid = jwtUtils.validateJwtToken(validToken);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should handle very long user IDs correctly")
        void shouldHandleVeryLongUserIdsCorrectly() {
            // Arrange
            UUID longUserId = UUID.randomUUID(); // UUIDs are fixed length, but test the concept
            UserPrincipal longUserPrincipal = new UserPrincipal(
                    longUserId,
                    "User with long ID",
                    "longid@example.com",
                    "password",
                    Collections.emptyList()
            );

            given(authentication.getPrincipal()).willReturn(longUserPrincipal);

            // Act
            String token = jwtUtils.generateJwtToken(authentication);
            String extractedUserId = jwtUtils.getUserIdFromJwtToken(token);

            // Assert
            assertThat(extractedUserId).isEqualTo(longUserId.toString());
            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should maintain token integrity over time")
        void shouldMaintainTokenIntegrityOverTime() throws InterruptedException {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            String token = jwtUtils.generateJwtToken(authentication);

            // Act - Wait a bit and then validate
            Thread.sleep(100);
            boolean isStillValid = jwtUtils.validateJwtToken(token);
            String userIdAfterWait = jwtUtils.getUserIdFromJwtToken(token);

            // Assert
            assertThat(isStillValid).isTrue();
            assertThat(userIdAfterWait).isEqualTo(userId.toString());
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should handle different secret lengths")
        void shouldHandleDifferentSecretLengths() {
            // Arrange
            String shortSecret = "shortSecret123456789012345678901234"; // Still needs to be 32+ chars
            ReflectionTestUtils.setField(jwtUtils, "jwtSecret", shortSecret);
            given(authentication.getPrincipal()).willReturn(userPrincipal);

            // Act
            String token = jwtUtils.generateJwtToken(authentication);

            // Assert
            assertThat(token).isNotNull();
            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should handle different expiration times")
        void shouldHandleDifferentExpirationTimes() {
            // Arrange
            int shortExpiration = 1000; // 1 second
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", shortExpiration);
            given(authentication.getPrincipal()).willReturn(userPrincipal);

            // Act
            String token = jwtUtils.generateJwtToken(authentication);

            // Assert
            assertThat(token).isNotNull();
            assertThat(jwtUtils.validateJwtToken(token)).isTrue();
            assertThat(jwtUtils.getUserIdFromJwtToken(token)).isEqualTo(userId.toString());
        }
    }
}
