package com.example.demo.security;

import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for UserPrincipal
 * Tests Spring Security UserDetails implementation and user authentication details
 * Covers authority mapping, account status, and user creation from entity
 */
@DisplayName("UserPrincipal Unit Tests")
class UserPrincipalTest {

    private User testUser;
    private User adminUser;
    private UUID testUserId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();

        testUser = User.builder()
                .id(testUserId)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .id(adminUserId)
                .name("Admin User")
                .email("admin@example.com")
                .password("encodedAdminPassword")
                .role(User.Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("UserPrincipal Creation Tests")
    class UserPrincipalCreationTests {

        @Test
        @DisplayName("Should create UserPrincipal from regular user entity")
        void shouldCreateUserPrincipalFromRegularUserEntity() {
            // Act
            UserPrincipal userPrincipal = UserPrincipal.create(testUser);

            // Assert
            assertThat(userPrincipal).isNotNull();
            assertThat(userPrincipal.getId()).isEqualTo(testUserId);
            assertThat(userPrincipal.getName()).isEqualTo("Test User");
            assertThat(userPrincipal.getEmail()).isEqualTo("test@example.com");
            assertThat(userPrincipal.getPassword()).isEqualTo("encodedPassword");
            
            Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();
            assertThat(authorities).hasSize(1);
            assertThat(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"))).isTrue();
        }

        @Test
        @DisplayName("Should create UserPrincipal from admin user entity")
        void shouldCreateUserPrincipalFromAdminUserEntity() {
            // Act
            UserPrincipal userPrincipal = UserPrincipal.create(adminUser);

            // Assert
            assertThat(userPrincipal).isNotNull();
            assertThat(userPrincipal.getId()).isEqualTo(adminUserId);
            assertThat(userPrincipal.getName()).isEqualTo("Admin User");
            assertThat(userPrincipal.getEmail()).isEqualTo("admin@example.com");
            assertThat(userPrincipal.getPassword()).isEqualTo("encodedAdminPassword");
            
            Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();
            assertThat(authorities).hasSize(1);
            assertThat(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))).isTrue();
        }

        @Test
        @DisplayName("Should handle user with null fields gracefully")
        void shouldHandleUserWithNullFieldsGracefully() {
            // Arrange
            User userWithNulls = User.builder()
                    .id(UUID.randomUUID())
                    .name(null)
                    .email("test@example.com")
                    .password("password")
                    .role(User.Role.USER)
                    .build();

            // Act
            UserPrincipal userPrincipal = UserPrincipal.create(userWithNulls);

            // Assert
            assertThat(userPrincipal).isNotNull();
            assertThat(userPrincipal.getName()).isNull();
            assertThat(userPrincipal.getEmail()).isEqualTo("test@example.com");
            assertThat(userPrincipal.getAuthorities()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when creating from null user")
        void shouldThrowExceptionWhenCreatingFromNullUser() {
            // Act & Assert
            assertThatThrownBy(() -> UserPrincipal.create(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle user with special characters in name and email")
        void shouldHandleUserWithSpecialCharactersInNameAndEmail() {
            // Arrange
            User specialUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("José María O'Connor")
                    .email("josé.maría@example.co.uk")
                    .password("encodedPassword")
                    .role(User.Role.USER)
                    .build();

            // Act
            UserPrincipal userPrincipal = UserPrincipal.create(specialUser);

            // Assert
            assertThat(userPrincipal).isNotNull();
            assertThat(userPrincipal.getName()).isEqualTo("José María O'Connor");
            assertThat(userPrincipal.getEmail()).isEqualTo("josé.maría@example.co.uk");
            assertThat(userPrincipal.getUsername()).isEqualTo("josé.maría@example.co.uk");
        }
    }

    @Nested
    @DisplayName("UserDetails Implementation Tests")
    class UserDetailsImplementationTests {

        private UserPrincipal userPrincipal;
        private UserPrincipal adminPrincipal;

        @BeforeEach
        void setUp() {
            userPrincipal = UserPrincipal.create(testUser);
            adminPrincipal = UserPrincipal.create(adminUser);
        }

        @Test
        @DisplayName("Should return email as username")
        void shouldReturnEmailAsUsername() {
            // Act & Assert
            assertThat(userPrincipal.getUsername()).isEqualTo("test@example.com");
            assertThat(adminPrincipal.getUsername()).isEqualTo("admin@example.com");
        }

        @Test
        @DisplayName("Should return correct password")
        void shouldReturnCorrectPassword() {
            // Act & Assert
            assertThat(userPrincipal.getPassword()).isEqualTo("encodedPassword");
            assertThat(adminPrincipal.getPassword()).isEqualTo("encodedAdminPassword");
        }

        @Test
        @DisplayName("Should return correct authorities for regular user")
        void shouldReturnCorrectAuthoritiesForRegularUser() {
            // Act
            Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();

            // Assert
            assertThat(authorities).isNotNull();
            assertThat(authorities).hasSize(1);
            assertThat(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"))).isTrue();
            assertThat(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))).isFalse();
        }

        @Test
        @DisplayName("Should return correct authorities for admin user")
        void shouldReturnCorrectAuthoritiesForAdminUser() {
            // Act
            Collection<? extends GrantedAuthority> authorities = adminPrincipal.getAuthorities();

            // Assert
            assertThat(authorities).isNotNull();
            assertThat(authorities).hasSize(1);
            assertThat(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))).isTrue();
            assertThat(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"))).isFalse();
        }

        @Test
        @DisplayName("Should indicate account is not expired")
        void shouldIndicateAccountIsNotExpired() {
            // Act & Assert
            assertThat(userPrincipal.isAccountNonExpired()).isTrue();
            assertThat(adminPrincipal.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("Should indicate account is not locked")
        void shouldIndicateAccountIsNotLocked() {
            // Act & Assert
            assertThat(userPrincipal.isAccountNonLocked()).isTrue();
            assertThat(adminPrincipal.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("Should indicate credentials are not expired")
        void shouldIndicateCredentialsAreNotExpired() {
            // Act & Assert
            assertThat(userPrincipal.isCredentialsNonExpired()).isTrue();
            assertThat(adminPrincipal.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("Should indicate account is enabled")
        void shouldIndicateAccountIsEnabled() {
            // Act & Assert
            assertThat(userPrincipal.isEnabled()).isTrue();
            assertThat(adminPrincipal.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Direct Constructor Tests")
    class DirectConstructorTests {

        @Test
        @DisplayName("Should create UserPrincipal with direct constructor")
        void shouldCreateUserPrincipalWithDirectConstructor() {
            // Arrange
            Collection<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_TEST")
            );

            // Act
            UserPrincipal userPrincipal = new UserPrincipal(
                    testUserId,
                    "Direct Test User",
                    "direct@example.com",
                    "directPassword",
                    authorities
            );

            // Assert
            assertThat(userPrincipal).isNotNull();
            assertThat(userPrincipal.getId()).isEqualTo(testUserId);
            assertThat(userPrincipal.getName()).isEqualTo("Direct Test User");
            assertThat(userPrincipal.getEmail()).isEqualTo("direct@example.com");
            assertThat(userPrincipal.getPassword()).isEqualTo("directPassword");
            assertThat(userPrincipal.getAuthorities()).hasSize(1);
            assertThat(userPrincipal.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_TEST"))).isTrue();
            assertThat(userPrincipal.getUsername()).isEqualTo("direct@example.com");
        }

        @Test
        @DisplayName("Should handle empty authorities collection")
        void shouldHandleEmptyAuthoritiesCollection() {
            // Arrange
            Collection<GrantedAuthority> emptyAuthorities = Collections.emptyList();

            // Act
            UserPrincipal userPrincipal = new UserPrincipal(
                    testUserId,
                    "Test User",
                    "test@example.com",
                    "password",
                    emptyAuthorities
            );

            // Assert
            assertThat(userPrincipal.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple authorities")
        void shouldHandleMultipleAuthorities() {
            // Arrange
            Collection<GrantedAuthority> multipleAuthorities = java.util.Arrays.asList(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_MODERATOR")
            );

            // Act
            UserPrincipal userPrincipal = new UserPrincipal(
                    testUserId,
                    "Multi Role User",
                    "multi@example.com",
                    "password",
                    multipleAuthorities
            );

            // Assert
            assertThat(userPrincipal.getAuthorities()).hasSize(3);
            Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();
            assertThat(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"))).isTrue();
            assertThat(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))).isTrue();
            assertThat(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_MODERATOR"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields are the same")
        void shouldBeEqualWhenAllFieldsAreTheSame() {
            // Arrange
            Collection<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            UserPrincipal principal1 = new UserPrincipal(
                    testUserId, "Test User", "test@example.com", "password", authorities
            );

            UserPrincipal principal2 = new UserPrincipal(
                    testUserId, "Test User", "test@example.com", "password", authorities
            );

            // Act & Assert
            assertThat(principal1).isEqualTo(principal2);
            assertThat(principal1.hashCode()).isEqualTo(principal2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when IDs are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            // Arrange
            Collection<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            UserPrincipal principal1 = new UserPrincipal(
                    testUserId, "Test User", "test@example.com", "password", authorities
            );

            UserPrincipal principal2 = new UserPrincipal(
                    UUID.randomUUID(), "Test User", "test@example.com", "password", authorities
            );

            // Act & Assert
            assertThat(principal1).isNotEqualTo(principal2);
        }

        @Test
        @DisplayName("Should not be equal when emails are different")
        void shouldNotBeEqualWhenEmailsAreDifferent() {
            // Arrange
            Collection<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            UserPrincipal principal1 = new UserPrincipal(
                    testUserId, "Test User", "test@example.com", "password", authorities
            );

            UserPrincipal principal2 = new UserPrincipal(
                    testUserId, "Test User", "different@example.com", "password", authorities
            );

            // Act & Assert
            assertThat(principal1).isNotEqualTo(principal2);
        }

        @Test
        @DisplayName("Should handle null values in equality comparison")
        void shouldHandleNullValuesInEqualityComparison() {
            // Arrange
            UserPrincipal principal1 = new UserPrincipal(
                    testUserId, null, "test@example.com", "password", Collections.emptyList()
            );

            UserPrincipal principal2 = new UserPrincipal(
                    testUserId, null, "test@example.com", "password", Collections.emptyList()
            );

            // Act & Assert
            assertThat(principal1).isEqualTo(principal2);
        }
    }

    @Nested
    @DisplayName("Security Integration Tests")
    class SecurityIntegrationTests {

        @Test
        @DisplayName("Should work with Spring Security authentication")
        void shouldWorkWithSpringSecurityAuthentication() {
            // Arrange
            UserPrincipal userPrincipal = UserPrincipal.create(testUser);

            // Act & Assert - Verify it implements UserDetails correctly
            assertThat(userPrincipal).isInstanceOf(org.springframework.security.core.userdetails.UserDetails.class);
            
            // Verify all required UserDetails methods return expected values
            assertThat(userPrincipal.getUsername()).isNotNull();
            assertThat(userPrincipal.getPassword()).isNotNull();
            assertThat(userPrincipal.getAuthorities()).isNotNull();
            assertThat(userPrincipal.isAccountNonExpired()).isTrue();
            assertThat(userPrincipal.isAccountNonLocked()).isTrue();
            assertThat(userPrincipal.isCredentialsNonExpired()).isTrue();
            assertThat(userPrincipal.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should provide correct role-based authorities")
        void shouldProvideCorrectRoleBasedAuthorities() {
            // Arrange
            UserPrincipal userPrincipal = UserPrincipal.create(testUser);
            UserPrincipal adminPrincipal = UserPrincipal.create(adminUser);

            // Act
            Collection<? extends GrantedAuthority> userAuthorities = userPrincipal.getAuthorities();
            Collection<? extends GrantedAuthority> adminAuthorities = adminPrincipal.getAuthorities();

            // Assert
            assertThat(userAuthorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"))).isTrue();
            assertThat(userAuthorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))).isFalse();
            
            assertThat(adminAuthorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))).isTrue();
            assertThat(adminAuthorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"))).isFalse();
        }

        @Test
        @DisplayName("Should maintain immutability of authorities")
        void shouldMaintainImmutabilityOfAuthorities() {
            // Arrange
            UserPrincipal userPrincipal = UserPrincipal.create(testUser);

            // Act
            Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();

            // Assert - Authorities should be immutable
            assertThatThrownBy(() -> {
                // This should throw an exception if the collection is immutable
                @SuppressWarnings("unchecked")
                Collection<GrantedAuthority> mutableAuthorities = (Collection<GrantedAuthority>) authorities;
                mutableAuthorities.add(new SimpleGrantedAuthority("ROLE_NEW"));
            }).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle very long user names and emails")
        void shouldHandleVeryLongUserNamesAndEmails() {
            // Arrange
            String veryLongName = "A".repeat(1000);
            String veryLongEmail = "a".repeat(50) + "@" + "b".repeat(50) + ".com";
            
            User longDataUser = User.builder()
                    .id(UUID.randomUUID())
                    .name(veryLongName)
                    .email(veryLongEmail)
                    .password("password")
                    .role(User.Role.USER)
                    .build();

            // Act
            UserPrincipal userPrincipal = UserPrincipal.create(longDataUser);

            // Assert
            assertThat(userPrincipal.getName()).isEqualTo(veryLongName);
            assertThat(userPrincipal.getEmail()).isEqualTo(veryLongEmail);
            assertThat(userPrincipal.getUsername()).isEqualTo(veryLongEmail);
        }

        @Test
        @DisplayName("Should handle special Unicode characters")
        void shouldHandleSpecialUnicodeCharacters() {
            // Arrange
            User unicodeUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("用户测试 🌟")
                    .email("тест@пример.рф")
                    .password("пароль123")
                    .role(User.Role.USER)
                    .build();

            // Act
            UserPrincipal userPrincipal = UserPrincipal.create(unicodeUser);

            // Assert
            assertThat(userPrincipal.getName()).isEqualTo("用户测试 🌟");
            assertThat(userPrincipal.getEmail()).isEqualTo("тест@пример.рф");
            assertThat(userPrincipal.getUsername()).isEqualTo("тест@пример.рф");
        }

        @Test
        @DisplayName("Should handle empty string values")
        void shouldHandleEmptyStringValues() {
            // Arrange
            User emptyStringUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("")
                    .email("")
                    .password("")
                    .role(User.Role.USER)
                    .build();

            // Act
            UserPrincipal userPrincipal = UserPrincipal.create(emptyStringUser);

            // Assert
            assertThat(userPrincipal.getName()).isEmpty();
            assertThat(userPrincipal.getEmail()).isEmpty();
            assertThat(userPrincipal.getUsername()).isEmpty();
            assertThat(userPrincipal.getPassword()).isEmpty();
        }
    }
}
