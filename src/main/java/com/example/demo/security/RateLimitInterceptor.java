package com.example.demo.security;

import com.example.demo.config.RateLimitConfig;
import com.example.demo.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> windowStartTimes = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!rateLimitConfig.isRateLimitEnabled()) {
            return true;
        }

        String clientId = getClientIdentifier(request);
        long currentTime = System.currentTimeMillis();
        long windowStart = windowStartTimes.computeIfAbsent(clientId, k -> new AtomicLong(currentTime)).get();

        // Reset window if 1 minute has passed
        if (currentTime - windowStart > 60000) {
            windowStartTimes.put(clientId, new AtomicLong(currentTime));
            requestCounts.put(clientId, new AtomicInteger(0));
        }

        int requestCount = requestCounts.computeIfAbsent(clientId, k -> new AtomicInteger(0)).incrementAndGet();

        if (requestCount > rateLimitConfig.getRequestsPerMinute()) {
            log.warn("Rate limit exceeded for client: {}", clientId);
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }

        return true;
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get user ID from JWT token, fall back to IP
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extract user ID from token if available
            return "user_" + request.getRemoteAddr(); // Simplified - you'd extract actual user ID
        }
        return "ip_" + request.getRemoteAddr();
    }
}
