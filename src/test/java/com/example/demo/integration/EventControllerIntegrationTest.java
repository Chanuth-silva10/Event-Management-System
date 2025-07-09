package com.example.demo.integration;

import com.example.demo.domain.dto.request.CreateUserRequest;
import com.example.demo.domain.dto.request.LoginRequest;
import com.example.demo.domain.dto.request.CreateEventRequest;
import com.example.demo.domain.entity.Event;
import com.example.demo.domain.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class EventControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String jwtToken;

    @BeforeEach
    public void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Create and login user to get JWT token
        CreateUserRequest registerRequest = CreateUserRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password123")
                .role(User.Role.USER)
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        // Extract token from response (simplified - you'd parse JSON properly)
        jwtToken = "Bearer " + extractTokenFromResponse(loginResponse);
    }

    @Test
    public void testCreateEvent() throws Exception {
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .location("Test Location")
                .visibility(Event.Visibility.PUBLIC)
                .build();

        mockMvc.perform(post("/events")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.location").value("Test Location"));
    }

    @Test
    public void testGetUpcomingEvents() throws Exception {
        mockMvc.perform(get("/events/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    public void testCreateEventWithoutAuth() throws Exception {
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .location("Test Location")
                .visibility(Event.Visibility.PUBLIC)
                .build();

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private String extractTokenFromResponse(String response) {
        // Simplified token extraction - in real implementation, parse JSON properly
        try {
            return response.split("\"token\":\"")[1].split("\"")[0];
        } catch (Exception e) {
            return "";
        }
    }
}
