package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Configuration class to handle SpringDoc configuration
 * and disable HATEOAS integration to avoid compatibility issues
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    org.springdoc.core.configuration.SpringDocHateoasConfiguration.class
})
public class SpringDocConfig {

    /**
     * This configuration class is used to disable SpringDoc HATEOAS integration
     * to avoid compatibility issues with Spring Boot 3.5.x and SpringDoc 2.8.0
     */
    
}
