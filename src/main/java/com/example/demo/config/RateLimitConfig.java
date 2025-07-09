package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RateLimitConfig {

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.requests-per-minute:100}")
    private long requestsPerMinute;

    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public long getRequestsPerMinute() {
        return requestsPerMinute;
    }
}
