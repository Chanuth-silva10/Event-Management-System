package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }
        
        if (authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication principal cannot be null");
        }
        
        if (!(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new ClassCastException("Principal must be an instance of UserPrincipal");
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Use current time with nanoseconds to ensure uniqueness
        long currentTimeMs = System.currentTimeMillis();
        long nanoTime = System.nanoTime();
        
        return Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .issuedAt(new Date(currentTimeMs))
                .expiration(new Date(currentTimeMs + jwtExpirationMs))
                .claim("nonce", nanoTime) // Add unique nonce to ensure different tokens
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserIdFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            log.error("JWT token is null or empty");
            return false;
        }
        
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
