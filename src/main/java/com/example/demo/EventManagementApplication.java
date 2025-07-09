package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Event Management System - Dialog Assignment
 * 
 * A comprehensive event management platform that provides:
 * - User registration and JWT-based authentication
 * - Event creation, management, and discovery
 * - RSVP and attendance tracking
 * - Role-based access control (USER/ADMIN)
 * - Caching for improved performance
 * - Rate limiting for API protection
 * 
 * Built with Spring Boot 3, Spring Security 6, PostgreSQL, and modern Java practices.
 * 
 * @author Chanuth Silva
 * @version 1.0.0
 * @since 2025-07-09
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
@Slf4j
public class EventManagementApplication {

	public static void main(String[] args) {
		try {
			ConfigurableApplicationContext context = SpringApplication.run(EventManagementApplication.class, args);
			
			String serverPort = context.getEnvironment().getProperty("server.port", "8080");
			String contextPath = context.getEnvironment().getProperty("server.servlet.context-path", "");
			
			log.info("=================================================================");
			log.info("Event Management System started successfully!");
			log.info("API Base URL: http://localhost:{}{}", serverPort, contextPath);
			log.info("API Documentation: http://localhost:{}{}/swagger-ui.html", serverPort, contextPath);
			log.info("Health Check: http://localhost:{}{}/actuator/health", serverPort, contextPath);
			log.info("=================================================================");
			log.info("Available Endpoints:");
			log.info("   • POST /auth/register - Register new user");
			log.info("   • POST /auth/login - User authentication");
			log.info("   • GET  /events - List public events");
			log.info("   • POST /events - Create new event");
			log.info("   • GET  /events/upcoming - Get upcoming events");
			log.info("   • POST /attendances - RSVP to events");
			log.info("=================================================================");
			
		} catch (Exception e) {
			log.error("Failed to start Event Management System", e);
			System.exit(1);
		}
	}
}
