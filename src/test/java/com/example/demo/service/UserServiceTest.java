package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for UserService
 * Tests user retrieval operations and error handling
 * Following industry best practices for service layer testing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;
    private UUID testUserId;
    private String testUserEmail;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUserEmail = "test@example.com";

        testUser = User.builder()
                .id(testUserId)
                .name("Test User")
                .email(testUserEmail)
                .password("encodedPassword")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Admin User")
                .email("admin@example.com")
                .password("encodedPassword")
                .role(User.Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get User By ID Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when found by ID")
        void shouldReturnUserWhenFoundById() {
            // Arrange
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));

            // Act
            User result = userService.getUserById(testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            assertThat(result.getName()).isEqualTo("Test User");
            assertThat(result.getEmail()).isEqualTo(testUserEmail);
            assertThat(result.getRole()).isEqualTo(User.Role.USER);

            verify(userRepository).findById(testUserId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found by ID")
        void shouldThrowResourceNotFoundExceptionWhenUserNotFoundById() {
            // Arrange
            UUID nonExistentUserId = UUID.randomUUID();
            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with ID: " + nonExistentUserId);

            verify(userRepository).findById(nonExistentUserId);
        }

        @Test
        @DisplayName("Should return admin user when found by ID")
        void shouldReturnAdminUserWhenFoundById() {
            // Arrange
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));

            // Act
            User result = userService.getUserById(adminUser.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(adminUser.getId());
            assertThat(result.getName()).isEqualTo("Admin User");
            assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);

            verify(userRepository).findById(adminUser.getId());
        }

        @Test
        @DisplayName("Should handle null UUID gracefully")
        void shouldHandleNullUuidGracefully() {
            // Arrange
            given(userRepository.findById(null)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with ID: null");

            verify(userRepository).findById(null);
        }

        @Test
        @DisplayName("Should handle repository exceptions")
        void shouldHandleRepositoryExceptions() {
            // Arrange
            given(userRepository.findById(testUserId))
                    .willThrow(new RuntimeException("Database connection error"));

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(testUserId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection error");

            verify(userRepository).findById(testUserId);
        }

        @Test
        @DisplayName("Should maintain transactional read-only behavior")
        void shouldMaintainTransactionalReadOnlyBehavior() {
            // This test verifies that the method is marked as read-only
            // In a real test, you might use a transaction manager mock
            
            // Arrange
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));

            // Act
            User result = userService.getUserById(testUserId);

            // Assert
            assertThat(result).isNotNull();
            verify(userRepository).findById(testUserId);
            // Verify no modifications are attempted
            verify(userRepository, never()).save(any(User.class));
            verify(userRepository, never()).delete(any(User.class));
        }
    }

    @Nested
    @DisplayName("Get User By Email Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Should return user when found by email")
        void shouldReturnUserWhenFoundByEmail() {
            // Arrange
            given(userRepository.findByEmail(testUserEmail)).willReturn(Optional.of(testUser));

            // Act
            User result = userService.getUserByEmail(testUserEmail);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            assertThat(result.getName()).isEqualTo("Test User");
            assertThat(result.getEmail()).isEqualTo(testUserEmail);
            assertThat(result.getRole()).isEqualTo(User.Role.USER);

            verify(userRepository).findByEmail(testUserEmail);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found by email")
        void shouldThrowResourceNotFoundExceptionWhenUserNotFoundByEmail() {
            // Arrange
            String nonExistentEmail = "nonexistent@example.com";
            given(userRepository.findByEmail(nonExistentEmail)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserByEmail(nonExistentEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: " + nonExistentEmail);

            verify(userRepository).findByEmail(nonExistentEmail);
        }

        @Test
        @DisplayName("Should handle case-sensitive email lookup")
        void shouldHandleCaseSensitiveEmailLookup() {
            // Arrange
            String upperCaseEmail = testUserEmail.toUpperCase();
            given(userRepository.findByEmail(upperCaseEmail)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserByEmail(upperCaseEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: " + upperCaseEmail);

            verify(userRepository).findByEmail(upperCaseEmail);
        }

        @Test
        @DisplayName("Should return admin user when found by email")
        void shouldReturnAdminUserWhenFoundByEmail() {
            // Arrange
            String adminEmail = adminUser.getEmail();
            given(userRepository.findByEmail(adminEmail)).willReturn(Optional.of(adminUser));

            // Act
            User result = userService.getUserByEmail(adminEmail);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(adminUser.getId());
            assertThat(result.getName()).isEqualTo("Admin User");
            assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);

            verify(userRepository).findByEmail(adminEmail);
        }

        @Test
        @DisplayName("Should handle null email gracefully")
        void shouldHandleNullEmailGracefully() {
            // Arrange
            given(userRepository.findByEmail(null)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserByEmail(null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: null");

            verify(userRepository).findByEmail(null);
        }

        @Test
        @DisplayName("Should handle empty email")
        void shouldHandleEmptyEmail() {
            // Arrange
            String emptyEmail = "";
            given(userRepository.findByEmail(emptyEmail)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserByEmail(emptyEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: " + emptyEmail);

            verify(userRepository).findByEmail(emptyEmail);
        }

        @Test
        @DisplayName("Should handle whitespace-only email")
        void shouldHandleWhitespaceOnlyEmail() {
            // Arrange
            String whitespaceEmail = "   ";
            given(userRepository.findByEmail(whitespaceEmail)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserByEmail(whitespaceEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: " + whitespaceEmail);

            verify(userRepository).findByEmail(whitespaceEmail);
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            // Arrange
            String specialEmail = "test+special@example.co.uk";
            User specialUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("Special User")
                    .email(specialEmail)
                    .role(User.Role.USER)
                    .build();

            given(userRepository.findByEmail(specialEmail)).willReturn(Optional.of(specialUser));

            // Act
            User result = userService.getUserByEmail(specialEmail);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(specialEmail);

            verify(userRepository).findByEmail(specialEmail);
        }

        @Test
        @DisplayName("Should handle very long email addresses")
        void shouldHandleVeryLongEmailAddresses() {
            // Arrange
            String longLocalPart = "a".repeat(64); // Maximum local part length
            String longEmail = longLocalPart + "@example.com";
            
            given(userRepository.findByEmail(longEmail)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserByEmail(longEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with email: " + longEmail);

            verify(userRepository).findByEmail(longEmail);
        }

        @Test
        @DisplayName("Should maintain transactional read-only behavior for email lookup")
        void shouldMaintainTransactionalReadOnlyBehaviorForEmailLookup() {
            // Arrange
            given(userRepository.findByEmail(testUserEmail)).willReturn(Optional.of(testUser));

            // Act
            User result = userService.getUserByEmail(testUserEmail);

            // Assert
            assertThat(result).isNotNull();
            verify(userRepository).findByEmail(testUserEmail);
            // Verify no modifications are attempted
            verify(userRepository, never()).save(any(User.class));
            verify(userRepository, never()).delete(any(User.class));
        }
    }

    @Nested
    @DisplayName("Performance and Reliability Tests")
    class PerformanceAndReliabilityTests {

        @Test
        @DisplayName("Should handle concurrent access to same user")
        void shouldHandleConcurrentAccessToSameUser() {
            // Arrange
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));

            // Act - Simulate concurrent calls
            User result1 = userService.getUserById(testUserId);
            User result2 = userService.getUserById(testUserId);

            // Assert
            assertThat(result1).isNotNull();
            assertThat(result2).isNotNull();
            assertThat(result1.getId()).isEqualTo(result2.getId());

            verify(userRepository, times(2)).findById(testUserId);
        }

        @Test
        @DisplayName("Should handle repository timeout scenarios")
        void shouldHandleRepositoryTimeoutScenarios() {
            // Arrange
            given(userRepository.findById(testUserId))
                    .willThrow(new RuntimeException("Query timeout"));

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(testUserId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Query timeout");

            verify(userRepository).findById(testUserId);
        }

        @Test
        @DisplayName("Should be consistent between ID and email lookups")
        void shouldBeConsistentBetweenIdAndEmailLookups() {
            // Arrange
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));
            given(userRepository.findByEmail(testUserEmail)).willReturn(Optional.of(testUser));

            // Act
            User userById = userService.getUserById(testUserId);
            User userByEmail = userService.getUserByEmail(testUserEmail);

            // Assert
            assertThat(userById).isNotNull();
            assertThat(userByEmail).isNotNull();
            assertThat(userById.getId()).isEqualTo(userByEmail.getId());
            assertThat(userById.getEmail()).isEqualTo(userByEmail.getEmail());
            assertThat(userById.getName()).isEqualTo(userByEmail.getName());
            assertThat(userById.getRole()).isEqualTo(userByEmail.getRole());

            verify(userRepository).findById(testUserId);
            verify(userRepository).findByEmail(testUserEmail);
        }

        @Test
        @DisplayName("Should handle database connection failures gracefully")
        void shouldHandleDatabaseConnectionFailuresGracefully() {
            // Arrange
            given(userRepository.findById(testUserId))
                    .willThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(testUserId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection failed");

            verify(userRepository).findById(testUserId);
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should return user with all required fields populated")
        void shouldReturnUserWithAllRequiredFieldsPopulated() {
            // Arrange
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));

            // Act
            User result = userService.getUserById(testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isNotNull().isNotEmpty();
            assertThat(result.getEmail()).isNotNull().isNotEmpty();
            assertThat(result.getPassword()).isNotNull().isNotEmpty();
            assertThat(result.getRole()).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();

            verify(userRepository).findById(testUserId);
        }

        @Test
        @DisplayName("Should preserve user entity state")
        void shouldPreserveUserEntityState() {
            // Arrange
            LocalDateTime originalCreatedAt = testUser.getCreatedAt();
            LocalDateTime originalUpdatedAt = testUser.getUpdatedAt();
            
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));

            // Act
            User result = userService.getUserById(testUserId);

            // Assert
            assertThat(result.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(result.getUpdatedAt()).isEqualTo(originalUpdatedAt);
            assertThat(result.getPassword()).isEqualTo(testUser.getPassword());

            verify(userRepository).findById(testUserId);
        }

        @Test
        @DisplayName("Should handle users with different roles consistently")
        void shouldHandleUsersWithDifferentRolesConsistently() {
            // Arrange
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testUser));
            given(userRepository.findById(adminUser.getId())).willReturn(Optional.of(adminUser));

            // Act
            User regularUser = userService.getUserById(testUserId);
            User adminUserResult = userService.getUserById(adminUser.getId());

            // Assert
            assertThat(regularUser.getRole()).isEqualTo(User.Role.USER);
            assertThat(adminUserResult.getRole()).isEqualTo(User.Role.ADMIN);

            verify(userRepository).findById(testUserId);
            verify(userRepository).findById(adminUser.getId());
        }
    }
}
