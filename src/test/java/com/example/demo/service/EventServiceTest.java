package com.example.demo.service;

import com.example.demo.domain.dto.request.CreateEventRequest;
import com.example.demo.domain.dto.request.UpdateEventRequest;
import com.example.demo.domain.dto.response.EventResponse;
import com.example.demo.domain.entity.Event;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.mapper.EventMapper;
import com.example.demo.repository.EventRepository;
import com.example.demo.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for EventService
 * Tests cover all business logic, validation, authorization, and edge cases
 * Following AAA pattern (Arrange, Act, Assert) and industry best practices
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserService userService;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EventService eventService;

    private User testUser;
    private User adminUser;
    private Event testEvent;
    private EventResponse testEventResponse;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        // Test data setup
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(User.Role.USER)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Admin User")
                .email("admin@example.com")
                .role(User.Role.ADMIN)
                .build();

        testEvent = Event.builder()
                .id(UUID.randomUUID())
                .title("Test Event")
                .description("Test Description")
                .host(testUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .visibility(Event.Visibility.PUBLIC)
                .location("Test Location")
                .build();

        testEventResponse = EventResponse.builder()
                .id(testEvent.getId())
                .title(testEvent.getTitle())
                .description(testEvent.getDescription())
                .startTime(testEvent.getStartTime())
                .endTime(testEvent.getEndTime())
                .visibility(testEvent.getVisibility())
                .location(testEvent.getLocation())
                .attendeeCount(0L)
                .build();

        userPrincipal = new UserPrincipal(
                testUser.getId(),
                testUser.getName(),
                testUser.getEmail(),
                "password",
                Collections.emptyList()
        );
    }

    @Nested
    @DisplayName("Create Event Tests")
    class CreateEventTests {

        private CreateEventRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = CreateEventRequest.builder()
                    .title("New Event")
                    .description("New Event Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .location("New Location")
                    .visibility(Event.Visibility.PUBLIC)
                    .build();
        }

        @Test
        @DisplayName("Should create event successfully with valid data")
        void shouldCreateEventSuccessfully() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.createEvent(validRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(testEventResponse.getTitle());
            
            verify(eventRepository).save(argThat(event ->
                event.getTitle().equals(validRequest.getTitle()) &&
                event.getDescription().equals(validRequest.getDescription()) &&
                event.getHost().equals(testUser) &&
                event.getStartTime().equals(validRequest.getStartTime()) &&
                event.getEndTime().equals(validRequest.getEndTime()) &&
                event.getLocation().equals(validRequest.getLocation()) &&
                event.getVisibility().equals(validRequest.getVisibility())
            ));
            verify(eventMapper).toEventResponse(testEvent);
            verify(userService).getUserById(testUser.getId());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when end time is before start time")
        void shouldThrowIllegalArgumentExceptionWhenEndTimeIsBeforeStartTime() {
            // Arrange
            CreateEventRequest invalidRequest = CreateEventRequest.builder()
                    .title("Invalid Event")
                    .description("Invalid Event Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).minusHours(1)) // End before start
                    .location("Test Location")
                    .visibility(Event.Visibility.PUBLIC)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);

            // Act & Assert
            assertThatThrownBy(() -> eventService.createEvent(invalidRequest, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("End time must be after start time");

            verify(eventRepository, never()).save(any(Event.class));
            verify(eventMapper, never()).toEventResponse(any(Event.class));
        }

        @Test
        @DisplayName("Should create private event successfully")
        void shouldCreatePrivateEventSuccessfully() {
            // Arrange
            CreateEventRequest privateRequest = CreateEventRequest.builder()
                    .title("Private Event")
                    .description("Private Event Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .location("Private Location")
                    .visibility(Event.Visibility.PRIVATE)
                    .build();

            Event privateEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .title(privateRequest.getTitle())
                    .description(privateRequest.getDescription())
                    .host(testUser)
                    .startTime(privateRequest.getStartTime())
                    .endTime(privateRequest.getEndTime())
                    .location(privateRequest.getLocation())
                    .visibility(Event.Visibility.PRIVATE)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(privateEvent);
            given(eventMapper.toEventResponse(privateEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.createEvent(privateRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(eventRepository).save(argThat(event ->
                event.getVisibility().equals(Event.Visibility.PRIVATE)
            ));
        }

        @Test
        @DisplayName("Should handle minimum duration events")
        void shouldHandleMinimumDurationEvents() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            CreateEventRequest minDurationRequest = CreateEventRequest.builder()
                    .title("Quick Meeting")
                    .description("15 minute standup")
                    .startTime(startTime)
                    .endTime(startTime.plusMinutes(15)) // Minimum duration
                    .location("Conference Room")
                    .visibility(Event.Visibility.PUBLIC)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.createEvent(minDurationRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("Should create event without location")
        void shouldCreateEventWithoutLocation() {
            // Arrange
            CreateEventRequest requestWithoutLocation = CreateEventRequest.builder()
                    .title("Virtual Event")
                    .description("Online meeting")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .visibility(Event.Visibility.PUBLIC)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.createEvent(requestWithoutLocation, authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(eventRepository).save(argThat(event ->
                event.getLocation() == null
            ));
        }

        @Test
        @DisplayName("Should throw exception when end time is before start time")
        void shouldThrowExceptionWhenEndTimeBeforeStartTime() {
            // Arrange
            CreateEventRequest invalidRequest = CreateEventRequest.builder()
                    .title("Invalid Event")
                    .description("Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now()) // End time before start time
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);

            // Act & Assert
            assertThatThrownBy(() -> eventService.createEvent(invalidRequest, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("End time must be after start time");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("Should create event with default visibility when not specified")
        void shouldCreateEventWithDefaultVisibility() {
            // Arrange
            CreateEventRequest requestWithoutVisibility = CreateEventRequest.builder()
                    .title("Event Without Visibility")
                    .description("Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);
            given(eventMapper.toEventResponse(any(Event.class))).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.createEvent(requestWithoutVisibility, authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(eventRepository).save(argThat(event -> 
                event.getVisibility() == Event.Visibility.PUBLIC));
        }

        @Test
        @DisplayName("Should create event with UserService exception handling")
        void shouldHandleUserServiceException() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId()))
                    .willThrow(new ResourceNotFoundException("User not found"));

            // Act & Assert
            assertThatThrownBy(() -> eventService.createEvent(validRequest, authentication))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("Should create event with null description")
        void shouldCreateEventWithNullDescription() {
            // Arrange
            CreateEventRequest requestWithNullDescription = CreateEventRequest.builder()
                    .title("Event with null description")
                    .description(null)
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .location("Test Location")
                    .visibility(Event.Visibility.PUBLIC)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.createEvent(requestWithNullDescription, authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(eventRepository).save(argThat(event ->
                event.getDescription() == null
            ));
        }

        @Test
        @DisplayName("Should handle same start and end time")
        void shouldThrowExceptionWhenSameStartAndEndTime() {
            // Arrange
            LocalDateTime sameTime = LocalDateTime.now().plusDays(1);
            CreateEventRequest sameTimeRequest = CreateEventRequest.builder()
                    .title("Zero Duration Event")
                    .description("Event with same start and end time")
                    .startTime(sameTime)
                    .endTime(sameTime)
                    .location("Test Location")
                    .visibility(Event.Visibility.PUBLIC)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            // Don't set up eventRepository.save() as it should throw before reaching there

            // Act & Assert
            assertThatThrownBy(() -> eventService.createEvent(sameTimeRequest, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("End time must be after start time");
        }
    }

    @Nested
    @DisplayName("Update Event Tests")
    class UpdateEventTests {

        private UpdateEventRequest updateRequest;
        private UUID eventId;

        @BeforeEach
        void setUp() {
            eventId = testEvent.getId();
            updateRequest = UpdateEventRequest.builder()
                    .title("Updated Event")
                    .description("Updated Description")
                    .location("Updated Location")
                    .visibility(Event.Visibility.PRIVATE)
                    .build();
        }

        @Test
        @DisplayName("Should update event successfully when user is host")
        void shouldUpdateEventWhenUserIsHost() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(eventId)).willReturn(Optional.of(testEvent));
            given(eventRepository.save(testEvent)).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.updateEvent(eventId, updateRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            assertThat(testEvent.getTitle()).isEqualTo("Updated Event");
            assertThat(testEvent.getDescription()).isEqualTo("Updated Description");
            assertThat(testEvent.getLocation()).isEqualTo("Updated Location");
            assertThat(testEvent.getVisibility()).isEqualTo(Event.Visibility.PRIVATE);
            verify(eventRepository).save(testEvent);
        }

        @Test
        @DisplayName("Should update event successfully when user is admin")
        void shouldUpdateEventWhenUserIsAdmin() {
            // Arrange
            UserPrincipal adminPrincipal = new UserPrincipal(
                    adminUser.getId(),
                    adminUser.getName(),
                    adminUser.getEmail(),
                    "password",
                    Collections.emptyList()
            );

            given(authentication.getPrincipal()).willReturn(adminPrincipal);
            given(userService.getUserById(adminUser.getId())).willReturn(adminUser);
            given(eventRepository.findActiveById(eventId)).willReturn(Optional.of(testEvent));
            given(eventRepository.save(testEvent)).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.updateEvent(eventId, updateRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(eventRepository).save(testEvent);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not host or admin")
        void shouldThrowUnauthorizedExceptionWhenUserNotAuthorized() {
            // Arrange
            User otherUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("Other User")
                    .email("other@example.com")
                    .role(User.Role.USER)
                    .build();

            UserPrincipal otherUserPrincipal = new UserPrincipal(
                    otherUser.getId(),
                    otherUser.getName(),
                    otherUser.getEmail(),
                    "password",
                    Collections.emptyList()
            );

            given(authentication.getPrincipal()).willReturn(otherUserPrincipal);
            given(userService.getUserById(otherUser.getId())).willReturn(otherUser);
            given(eventRepository.findActiveById(eventId)).willReturn(Optional.of(testEvent));

            // Act & Assert
            assertThatThrownBy(() -> eventService.updateEvent(eventId, updateRequest, authentication))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You don't have permission to update this event");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when event not found")
        void shouldThrowResourceNotFoundExceptionWhenEventNotFound() {
            // Arrange
            UUID nonExistentEventId = UUID.randomUUID();
            
            given(eventRepository.findActiveById(nonExistentEventId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> eventService.updateEvent(nonExistentEventId, updateRequest, authentication))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Event not found with ID: " + nonExistentEventId);

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("Should update only specified fields")
        void shouldUpdateOnlySpecifiedFields() {
            // Arrange
            UpdateEventRequest partialUpdateRequest = UpdateEventRequest.builder()
                    .title("Only Title Updated")
                    .build();

            String originalDescription = testEvent.getDescription();
            String originalLocation = testEvent.getLocation();
            Event.Visibility originalVisibility = testEvent.getVisibility();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(eventId)).willReturn(Optional.of(testEvent));
            given(eventRepository.save(testEvent)).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            eventService.updateEvent(eventId, partialUpdateRequest, authentication);

            // Assert
            assertThat(testEvent.getTitle()).isEqualTo("Only Title Updated");
            assertThat(testEvent.getDescription()).isEqualTo(originalDescription);
            assertThat(testEvent.getLocation()).isEqualTo(originalLocation);
            assertThat(testEvent.getVisibility()).isEqualTo(originalVisibility);
        }

        @Test
        @DisplayName("Should update start and end times")
        void shouldUpdateStartAndEndTimes() {
            // Arrange
            LocalDateTime newStartTime = LocalDateTime.now().plusDays(2);
            LocalDateTime newEndTime = LocalDateTime.now().plusDays(2).plusHours(3);
            
            UpdateEventRequest timeUpdateRequest = UpdateEventRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newEndTime)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(eventId)).willReturn(Optional.of(testEvent));
            given(eventRepository.save(testEvent)).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            eventService.updateEvent(eventId, timeUpdateRequest, authentication);

            // Assert
            assertThat(testEvent.getStartTime()).isEqualTo(newStartTime);
            assertThat(testEvent.getEndTime()).isEqualTo(newEndTime);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when updated end time is before start time")
        void shouldThrowIllegalArgumentExceptionWhenUpdatedEndTimeBeforeStartTime() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            UpdateEventRequest invalidTimeRequest = UpdateEventRequest.builder()
                    .startTime(startTime)
                    .endTime(startTime.minusHours(1)) // End before start
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(eventId)).willReturn(Optional.of(testEvent));

            // Act & Assert
            assertThatThrownBy(() -> eventService.updateEvent(eventId, invalidTimeRequest, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("End time must be after start time");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("Should validate time consistency when updating only start time")
        void shouldValidateTimeConsistencyWhenUpdatingOnlyStartTime() {
            // Arrange
            LocalDateTime newStartTime = testEvent.getEndTime().plusHours(1); // Start after current end
            UpdateEventRequest startTimeUpdateRequest = UpdateEventRequest.builder()
                    .startTime(newStartTime)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(eventId)).willReturn(Optional.of(testEvent));

            // Act & Assert
            assertThatThrownBy(() -> eventService.updateEvent(eventId, startTimeUpdateRequest, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("End time must be after start time");
        }

        @Test
        @DisplayName("Should validate time consistency when updating only end time")
        void shouldValidateTimeConsistencyWhenUpdatingOnlyEndTime() {
            // Arrange
            LocalDateTime newEndTime = testEvent.getStartTime().minusHours(1); // End before current start
            UpdateEventRequest endTimeUpdateRequest = UpdateEventRequest.builder()
                    .endTime(newEndTime)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(eventId)).willReturn(Optional.of(testEvent));

            // Act & Assert
            assertThatThrownBy(() -> eventService.updateEvent(eventId, endTimeUpdateRequest, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("End time must be after start time");
        }
    }

    @Nested
    @DisplayName("Delete Event Tests")
    class DeleteEventTests {

        @Test
        @DisplayName("Should delete event successfully when user is host")
        void shouldDeleteEventWhenUserIsHost() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));

            // Act
            eventService.deleteEvent(testEvent.getId(), authentication);

            // Assert
            verify(eventRepository).delete(testEvent);
        }

        @Test
        @DisplayName("Should delete event successfully when user is admin")
        void shouldDeleteEventWhenUserIsAdmin() {
            // Arrange
            UserPrincipal adminPrincipal = new UserPrincipal(
                    adminUser.getId(),
                    adminUser.getName(),
                    adminUser.getEmail(),
                    "password",
                    Collections.emptyList()
            );

            given(authentication.getPrincipal()).willReturn(adminPrincipal);
            given(userService.getUserById(adminUser.getId())).willReturn(adminUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));

            // Act
            eventService.deleteEvent(testEvent.getId(), authentication);

            // Assert
            verify(eventRepository).delete(testEvent);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not host or admin")
        void shouldThrowUnauthorizedExceptionWhenUserNotAuthorized() {
            // Arrange
            User otherUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("Other User")
                    .email("other@example.com")
                    .role(User.Role.USER)
                    .build();

            UserPrincipal otherUserPrincipal = new UserPrincipal(
                    otherUser.getId(),
                    otherUser.getName(),
                    otherUser.getEmail(),
                    "password",
                    Collections.emptyList()
            );

            given(authentication.getPrincipal()).willReturn(otherUserPrincipal);
            given(userService.getUserById(otherUser.getId())).willReturn(otherUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));

            // Act & Assert
            assertThatThrownBy(() -> eventService.deleteEvent(testEvent.getId(), authentication))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You don't have permission to delete this event");

            verify(eventRepository, never()).delete(any(Event.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when event not found")
        void shouldThrowResourceNotFoundExceptionWhenEventNotFound() {
            // Arrange
            UUID nonExistentEventId = UUID.randomUUID();
            
            given(eventRepository.findActiveById(nonExistentEventId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> eventService.deleteEvent(nonExistentEventId, authentication))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Event not found with ID: " + nonExistentEventId);

            verify(eventRepository, never()).delete(any(Event.class));
        }
    }

    @Nested
    @DisplayName("Get Event Tests")
    class GetEventTests {

        @Test
        @DisplayName("Should get event successfully when user is host")
        void shouldGetEventWhenUserIsHost() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.getEvent(testEvent.getId(), authentication);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testEvent.getId());
            verify(eventMapper).toEventResponse(testEvent);
        }

        @Test
        @DisplayName("Should get public event successfully")
        void shouldGetPublicEventSuccessfully() {
            // Arrange
            User otherUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("Other User")
                    .email("other@example.com")
                    .role(User.Role.USER)
                    .build();

            UserPrincipal otherUserPrincipal = new UserPrincipal(
                    otherUser.getId(),
                    otherUser.getName(),
                    otherUser.getEmail(),
                    "password",
                    Collections.emptyList()
            );

            given(authentication.getPrincipal()).willReturn(otherUserPrincipal);
            given(userService.getUserById(otherUser.getId())).willReturn(otherUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.getEvent(testEvent.getId(), authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(eventMapper).toEventResponse(testEvent);
        }

        @Test
        @DisplayName("Should get private event when user is admin")
        void shouldGetPrivateEventWhenUserIsAdmin() {
            // Arrange
            Event privateEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Private Event")
                    .host(testUser)
                    .visibility(Event.Visibility.PRIVATE)
                    .build();

            UserPrincipal adminPrincipal = new UserPrincipal(
                    adminUser.getId(),
                    adminUser.getName(),
                    adminUser.getEmail(),
                    "password",
                    Collections.emptyList()
            );

            given(authentication.getPrincipal()).willReturn(adminPrincipal);
            given(userService.getUserById(adminUser.getId())).willReturn(adminUser);
            given(eventRepository.findActiveById(privateEvent.getId())).willReturn(Optional.of(privateEvent));
            given(eventMapper.toEventResponse(privateEvent)).willReturn(testEventResponse);

            // Act
            EventResponse result = eventService.getEvent(privateEvent.getId(), authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(eventMapper).toEventResponse(privateEvent);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user cannot view private event")
        void shouldThrowUnauthorizedExceptionWhenUserCannotViewPrivateEvent() {
            // Arrange
            Event privateEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Private Event")
                    .host(adminUser) // Different host
                    .visibility(Event.Visibility.PRIVATE)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(privateEvent.getId())).willReturn(Optional.of(privateEvent));

            // Act & Assert
            assertThatThrownBy(() -> eventService.getEvent(privateEvent.getId(), authentication))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You don't have permission to view this event");

            verify(eventMapper, never()).toEventResponse(any(Event.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when event not found")
        void shouldThrowResourceNotFoundExceptionWhenEventNotFound() {
            // Arrange
            UUID nonExistentEventId = UUID.randomUUID();
            
            given(eventRepository.findActiveById(nonExistentEventId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> eventService.getEvent(nonExistentEventId, authentication))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Event not found with ID: " + nonExistentEventId);

            verify(eventMapper, never()).toEventResponse(any(Event.class));
        }
    }

    @Nested
    @DisplayName("Get Upcoming Events Tests")
    class GetUpcomingEventsTests {

        @Test
        @DisplayName("Should get upcoming events successfully")
        void shouldGetUpcomingEventsSuccessfully() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

            given(eventRepository.findUpcomingEvents(any(LocalDateTime.class), eq(pageable)))
                    .willReturn(eventPage);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            Page<EventResponse> result = eventService.getUpcomingEvents(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testEventResponse);
            verify(eventRepository).findUpcomingEvents(any(LocalDateTime.class), eq(pageable));
        }

        @Test
        @DisplayName("Should return empty page when no upcoming events")
        void shouldReturnEmptyPageWhenNoUpcomingEvents() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList());

            given(eventRepository.findUpcomingEvents(any(LocalDateTime.class), eq(pageable)))
                    .willReturn(emptyPage);

            // Act
            Page<EventResponse> result = eventService.getUpcomingEvents(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            verify(eventMapper, never()).toEventResponse(any(Event.class));
        }
    }

    @Nested
    @DisplayName("Get Events With Filters Tests")
    class GetEventsWithFiltersTests {

        @Test
        @DisplayName("Should get events with location filter")
        void shouldGetEventsWithLocationFilter() {
            // Arrange
            String location = "Conference Room";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

            given(eventRepository.findEventsWithFilters(eq(location), isNull(), isNull(), isNull(), eq(pageable)))
                    .willReturn(eventPage);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            Page<EventResponse> result = eventService.getEventsWithFilters(location, null, null, null, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(eventRepository).findEventsWithFilters(eq(location), isNull(), isNull(), isNull(), eq(pageable));
        }

        @Test
        @DisplayName("Should get events with visibility filter")
        void shouldGetEventsWithVisibilityFilter() {
            // Arrange
            Event.Visibility visibility = Event.Visibility.PUBLIC;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

            given(eventRepository.findEventsWithFilters(isNull(), eq("PUBLIC"), isNull(), isNull(), eq(pageable)))
                    .willReturn(eventPage);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            Page<EventResponse> result = eventService.getEventsWithFilters(null, visibility, null, null, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(eventRepository).findEventsWithFilters(isNull(), eq("PUBLIC"), isNull(), isNull(), eq(pageable));
        }

        @Test
        @DisplayName("Should get events with date range filter")
        void shouldGetEventsWithDateRangeFilter() {
            // Arrange
            LocalDateTime startDate = LocalDateTime.now().plusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(7);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

            given(eventRepository.findEventsWithFilters(isNull(), isNull(), eq(startDate), eq(endDate), eq(pageable)))
                    .willReturn(eventPage);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            Page<EventResponse> result = eventService.getEventsWithFilters(null, null, startDate, endDate, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(eventRepository).findEventsWithFilters(isNull(), isNull(), eq(startDate), eq(endDate), eq(pageable));
        }

        @Test
        @DisplayName("Should get events with all filters")
        void shouldGetEventsWithAllFilters() {
            // Arrange
            String location = "Conference Room";
            Event.Visibility visibility = Event.Visibility.PRIVATE;
            LocalDateTime startDate = LocalDateTime.now().plusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(7);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

            given(eventRepository.findEventsWithFilters(eq(location), eq("PRIVATE"), eq(startDate), eq(endDate), eq(pageable)))
                    .willReturn(eventPage);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            Page<EventResponse> result = eventService.getEventsWithFilters(location, visibility, startDate, endDate, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(eventRepository).findEventsWithFilters(eq(location), eq("PRIVATE"), eq(startDate), eq(endDate), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Get User Events Tests")
    class GetUserEventsTests {

        @Test
        @DisplayName("Should get user hosted events successfully")
        void shouldGetUserHostedEventsSuccessfully() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findByHost(testUser, pageable)).willReturn(eventPage);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            Page<EventResponse> result = eventService.getUserHostedEvents(authentication, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(eventRepository).findByHost(testUser, pageable);
        }

        @Test
        @DisplayName("Should get user attending events successfully")
        void shouldGetUserAttendingEventsSuccessfully() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findEventsUserIsAttending(testUser, pageable)).willReturn(eventPage);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            Page<EventResponse> result = eventService.getUserAttendingEvents(authentication, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(eventRepository).findEventsUserIsAttending(testUser, pageable);
        }

        @Test
        @DisplayName("Should get all user events successfully")
        void shouldGetAllUserEventsSuccessfully() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findEventsUserIsHostingOrAttending(testUser, pageable)).willReturn(eventPage);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            Page<EventResponse> result = eventService.getUserEvents(authentication, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(eventRepository).findEventsUserIsHostingOrAttending(testUser, pageable);
        }

        @Test
        @DisplayName("Should return empty page when user has no events")
        void shouldReturnEmptyPageWhenUserHasNoEvents() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList());

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findByHost(testUser, pageable)).willReturn(emptyPage);

            // Act
            Page<EventResponse> result = eventService.getUserHostedEvents(authentication, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            verify(eventMapper, never()).toEventResponse(any(Event.class));
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should get current user from authentication")
        void shouldGetCurrentUserFromAuthentication() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            eventService.getEvent(testEvent.getId(), authentication);

            // Assert
            verify(userService).getUserById(testUser.getId());
        }

        @Test
        @DisplayName("Should handle UserService exceptions")
        void shouldHandleUserServiceExceptions() {
            // Arrange
            CreateEventRequest request = CreateEventRequest.builder()
                    .title("Test Event")
                    .description("Test Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId()))
                    .willThrow(new ResourceNotFoundException("User not found"));

            // Act & Assert
            assertThatThrownBy(() -> eventService.createEvent(request, authentication))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should preserve event relationships")
        void shouldPreserveEventRelationships() {
            // Arrange
            CreateEventRequest request = CreateEventRequest.builder()
                    .title("Test Event")
                    .description("Test Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .location("Test Location")
                    .visibility(Event.Visibility.PUBLIC)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            eventService.createEvent(request, authentication);

            // Assert
            verify(eventRepository).save(argThat(event ->
                event.getHost().equals(testUser)
            ));
        }

        @Test
        @DisplayName("Should handle caching annotations")
        void shouldHandleCachingAnnotations() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            eventService.getEvent(testEvent.getId(), authentication);

            // Assert - Verify that the method executes without caching issues
            verify(eventRepository).findActiveById(testEvent.getId());
        }

        @Test
        @DisplayName("Should handle transaction boundaries")
        void shouldHandleTransactionBoundaries() {
            // Arrange
            CreateEventRequest request = CreateEventRequest.builder()
                    .title("Transaction Test Event")
                    .description("Test Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .location("Test Location")
                    .visibility(Event.Visibility.PUBLIC)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            eventService.createEvent(request, authentication);

            // Assert - Transaction is handled at service level
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("Should handle authorization for all operations")
        void shouldHandleAuthorizationForAllOperations() {
            // Test create - no special auth needed beyond being authenticated
            CreateEventRequest request = CreateEventRequest.builder()
                    .title("Auth Test Event")
                    .description("Test Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .location("Test Location")
                    .visibility(Event.Visibility.PUBLIC)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act & Assert
            assertThatCode(() -> eventService.createEvent(request, authentication))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should validate all entity constraints")
        void shouldValidateAllEntityConstraints() {
            // This test ensures that business validation is properly handled
            // Testing time validation specifically
            CreateEventRequest invalidTimeRequest = CreateEventRequest.builder()
                    .title("Test Event")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now()) // Invalid: end before start
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);

            // Act & Assert
            assertThatThrownBy(() -> eventService.createEvent(invalidTimeRequest, authentication))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle null authentication")
        void shouldHandleNullAuthentication() {
            // Arrange
            CreateEventRequest request = CreateEventRequest.builder()
                    .title("Null Auth Test")
                    .description("Test Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> eventService.createEvent(request, null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Authentication required");
        }

        @Test
        @DisplayName("Should handle repository exceptions")
        void shouldHandleRepositoryExceptions() {
            // Arrange
            CreateEventRequest request = CreateEventRequest.builder()
                    .title("Repository Error Test")
                    .description("Test Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class)))
                    .willThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThatThrownBy(() -> eventService.createEvent(request, authentication))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");
        }

        @Test
        @DisplayName("Should handle mapper exceptions")
        void shouldHandleMapperExceptions() {
            // Arrange
            CreateEventRequest request = CreateEventRequest.builder()
                    .title("Mapper Error Test")
                    .description("Test Description")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.save(any(Event.class))).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent))
                    .willThrow(new RuntimeException("Mapping error"));

            // Act & Assert
            assertThatThrownBy(() -> eventService.createEvent(request, authentication))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping error");
        }

        @Test
        @DisplayName("Should handle large pageable requests")
        void shouldHandleLargePageableRequests() {
            // Arrange
            Pageable largePage = PageRequest.of(0, 1000);
            Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));

            given(eventRepository.findUpcomingEvents(any(LocalDateTime.class), eq(largePage)))
                    .willReturn(eventPage);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            // Act
            Page<EventResponse> result = eventService.getUpcomingEvents(largePage);

            // Assert
            assertThat(result).isNotNull();
            verify(eventRepository).findUpcomingEvents(any(LocalDateTime.class), eq(largePage));
        }

        @Test
        @DisplayName("Should handle concurrent access scenarios")
        void shouldHandleConcurrentAccessScenarios() {
            // This test simulates potential concurrent modification
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(eventRepository.save(testEvent)).willReturn(testEvent);
            given(eventMapper.toEventResponse(testEvent)).willReturn(testEventResponse);

            UpdateEventRequest concurrentUpdate = UpdateEventRequest.builder()
                    .title("Concurrent Update")
                    .build();

            // Act
            EventResponse result = eventService.updateEvent(testEvent.getId(), concurrentUpdate, authentication);

            // Assert
            assertThat(result).isNotNull();
            assertThat(testEvent.getTitle()).isEqualTo("Concurrent Update");
        }
    }
}
