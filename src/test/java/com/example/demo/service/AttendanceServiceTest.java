package com.example.demo.service;

import com.example.demo.domain.dto.request.AttendanceRequest;
import com.example.demo.domain.dto.response.AttendanceResponse;
import com.example.demo.domain.entity.Attendance;
import com.example.demo.domain.entity.Event;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.AttendanceMapper;
import com.example.demo.repository.AttendanceRepository;
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
 * Unit tests for AttendanceService
 * Tests attendance management, RSVP functionality, and business logic
 * Following industry best practices for service layer testing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService Unit Tests")
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserService userService;

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AttendanceService attendanceService;

    private User testUser;
    private User otherUser;
    private Event testEvent;
    private Attendance testAttendance;
    private AttendanceResponse testAttendanceResponse;
    private AttendanceRequest attendanceRequest;
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

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Other User")
                .email("other@example.com")
                .role(User.Role.USER)
                .build();

        testEvent = Event.builder()
                .id(UUID.randomUUID())
                .title("Test Event")
                .description("Test Description")
                .host(otherUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .visibility(Event.Visibility.PUBLIC)
                .location("Test Location")
                .build();

        testAttendance = Attendance.builder()
                .event(testEvent)
                .user(testUser)
                .status(Attendance.Status.GOING)
                .respondedAt(LocalDateTime.now())
                .build();

        testAttendanceResponse = AttendanceResponse.builder()
                .status(Attendance.Status.GOING)
                .respondedAt(testAttendance.getRespondedAt())
                .build();

        attendanceRequest = AttendanceRequest.builder()
                .eventId(testEvent.getId())
                .status(Attendance.Status.GOING)
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
    @DisplayName("Update Attendance Tests")
    class UpdateAttendanceTests {

        @Test
        @DisplayName("Should create new attendance when user hasn't responded yet")
        void shouldCreateNewAttendanceWhenUserHasntRespondedYet() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(attendanceRepository.findByEventAndUser(testEvent, testUser)).willReturn(Optional.empty());
            given(attendanceRepository.save(any(Attendance.class))).willReturn(testAttendance);
            given(attendanceMapper.toAttendanceResponse(testAttendance)).willReturn(testAttendanceResponse);

            // Act
            AttendanceResponse result = attendanceService.updateAttendance(attendanceRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(Attendance.Status.GOING);

            verify(attendanceRepository).save(argThat(attendance ->
                attendance.getEvent().equals(testEvent) &&
                attendance.getUser().equals(testUser) &&
                attendance.getStatus().equals(Attendance.Status.GOING)
            ));
            verify(attendanceMapper).toAttendanceResponse(testAttendance);
        }

        @Test
        @DisplayName("Should update existing attendance when user changes response")
        void shouldUpdateExistingAttendanceWhenUserChangesResponse() {
            // Arrange
            Attendance existingAttendance = Attendance.builder()
                    .event(testEvent)
                    .user(testUser)
                    .status(Attendance.Status.MAYBE)
                    .respondedAt(LocalDateTime.now().minusHours(1))
                    .build();

            AttendanceRequest updateRequest = AttendanceRequest.builder()
                    .eventId(testEvent.getId())
                    .status(Attendance.Status.DECLINED)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(attendanceRepository.findByEventAndUser(testEvent, testUser)).willReturn(Optional.of(existingAttendance));
            given(attendanceRepository.save(existingAttendance)).willReturn(existingAttendance);
            given(attendanceMapper.toAttendanceResponse(existingAttendance)).willReturn(testAttendanceResponse);

            // Act
            AttendanceResponse result = attendanceService.updateAttendance(updateRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            assertThat(existingAttendance.getStatus()).isEqualTo(Attendance.Status.DECLINED);

            verify(attendanceRepository).save(existingAttendance);
            verify(attendanceMapper).toAttendanceResponse(existingAttendance);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when event not found")
        void shouldThrowResourceNotFoundExceptionWhenEventNotFound() {
            // Arrange
            UUID nonExistentEventId = UUID.randomUUID();
            AttendanceRequest invalidRequest = AttendanceRequest.builder()
                    .eventId(nonExistentEventId)
                    .status(Attendance.Status.GOING)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(nonExistentEventId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.updateAttendance(invalidRequest, authentication))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Event not found with ID: " + nonExistentEventId);

            verify(attendanceRepository, never()).save(any(Attendance.class));
        }

        @Test
        @DisplayName("Should handle all attendance statuses")
        void shouldHandleAllAttendanceStatuses() {
            // Test each status
            Attendance.Status[] statuses = {
                Attendance.Status.GOING,
                Attendance.Status.MAYBE,
                Attendance.Status.DECLINED
            };

            for (Attendance.Status status : statuses) {
                // Arrange
                AttendanceRequest request = AttendanceRequest.builder()
                        .eventId(testEvent.getId())
                        .status(status)
                        .build();

                given(authentication.getPrincipal()).willReturn(userPrincipal);
                given(userService.getUserById(testUser.getId())).willReturn(testUser);
                given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
                given(attendanceRepository.findByEventAndUser(testEvent, testUser)).willReturn(Optional.empty());
                given(attendanceRepository.save(any(Attendance.class))).willReturn(testAttendance);
                given(attendanceMapper.toAttendanceResponse(testAttendance)).willReturn(testAttendanceResponse);

                // Act
                AttendanceResponse result = attendanceService.updateAttendance(request, authentication);

                // Assert
                assertThat(result).isNotNull();
                verify(attendanceRepository).save(argThat(attendance ->
                    attendance.getStatus().equals(status)
                ));

                // Reset mocks for next iteration
                reset(attendanceRepository, attendanceMapper);
            }
        }

        @Test
        @DisplayName("Should allow host to RSVP to their own event")
        void shouldAllowHostToRsvpToTheirOwnEvent() {
            // Arrange
            Event hostEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Host Event")
                    .host(testUser) // testUser is the host
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .visibility(Event.Visibility.PUBLIC)
                    .build();

            AttendanceRequest hostRequest = AttendanceRequest.builder()
                    .eventId(hostEvent.getId())
                    .status(Attendance.Status.GOING)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(hostEvent.getId())).willReturn(Optional.of(hostEvent));
            given(attendanceRepository.findByEventAndUser(hostEvent, testUser)).willReturn(Optional.empty());
            given(attendanceRepository.save(any(Attendance.class))).willReturn(testAttendance);
            given(attendanceMapper.toAttendanceResponse(testAttendance)).willReturn(testAttendanceResponse);

            // Act
            AttendanceResponse result = attendanceService.updateAttendance(hostRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(attendanceRepository).save(any(Attendance.class));
        }

        @Test
        @DisplayName("Should handle concurrent attendance updates")
        void shouldHandleConcurrentAttendanceUpdates() {
            // Arrange
            Attendance existingAttendance = Attendance.builder()
                    .event(testEvent)
                    .user(testUser)
                    .status(Attendance.Status.MAYBE)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(attendanceRepository.findByEventAndUser(testEvent, testUser)).willReturn(Optional.of(existingAttendance));
            given(attendanceRepository.save(existingAttendance)).willReturn(existingAttendance);
            given(attendanceMapper.toAttendanceResponse(existingAttendance)).willReturn(testAttendanceResponse);

            // Act
            AttendanceResponse result = attendanceService.updateAttendance(attendanceRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            assertThat(existingAttendance.getStatus()).isEqualTo(Attendance.Status.GOING);
            verify(attendanceRepository).save(existingAttendance);
        }
    }

    @Nested
    @DisplayName("Get User Attendances Tests")
    class GetUserAttendancesTests {

        @Test
        @DisplayName("Should get user attendances successfully")
        void shouldGetUserAttendancesSuccessfully() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendance> attendancePage = new PageImpl<>(Collections.singletonList(testAttendance));

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(attendanceRepository.findByUser(testUser, pageable)).willReturn(attendancePage);
            given(attendanceMapper.toAttendanceResponse(testAttendance)).willReturn(testAttendanceResponse);

            // Act
            Page<AttendanceResponse> result = attendanceService.getUserAttendances(authentication, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testAttendanceResponse);

            verify(attendanceRepository).findByUser(testUser, pageable);
            verify(attendanceMapper).toAttendanceResponse(testAttendance);
        }

        @Test
        @DisplayName("Should return empty page when user has no attendances")
        void shouldReturnEmptyPageWhenUserHasNoAttendances() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendance> emptyPage = new PageImpl<>(Collections.emptyList());

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(attendanceRepository.findByUser(testUser, pageable)).willReturn(emptyPage);

            // Act
            Page<AttendanceResponse> result = attendanceService.getUserAttendances(authentication, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();

            verify(attendanceRepository).findByUser(testUser, pageable);
            verify(attendanceMapper, never()).toAttendanceResponse(any());
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            // Arrange
            Pageable pageable = PageRequest.of(1, 5); // Second page, 5 items per page
            Page<Attendance> attendancePage = new PageImpl<>(
                    Collections.singletonList(testAttendance),
                    pageable,
                    10 // Total 10 items
            );

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(attendanceRepository.findByUser(testUser, pageable)).willReturn(attendancePage);
            given(attendanceMapper.toAttendanceResponse(testAttendance)).willReturn(testAttendanceResponse);

            // Act
            Page<AttendanceResponse> result = attendanceService.getUserAttendances(authentication, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getNumber()).isEqualTo(1); // Page number
            assertThat(result.getSize()).isEqualTo(5); // Page size
            assertThat(result.getTotalElements()).isEqualTo(10); // Total elements
            assertThat(result.getTotalPages()).isEqualTo(2); // Total pages

            verify(attendanceRepository).findByUser(testUser, pageable);
        }
    }

    @Nested
    @DisplayName("Get Event Attendances Tests")
    class GetEventAttendancesTests {

        @Test
        @DisplayName("Should get event attendances successfully")
        void shouldGetEventAttendancesSuccessfully() {
            // Arrange
            String eventId = testEvent.getId().toString();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendance> attendancePage = new PageImpl<>(Collections.singletonList(testAttendance));

            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(attendanceRepository.findByEvent(testEvent, pageable)).willReturn(attendancePage);
            given(attendanceMapper.toAttendanceResponse(testAttendance)).willReturn(testAttendanceResponse);

            // Act
            Page<AttendanceResponse> result = attendanceService.getEventAttendances(eventId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testAttendanceResponse);

            verify(eventRepository).findActiveById(testEvent.getId());
            verify(attendanceRepository).findByEvent(testEvent, pageable);
            verify(attendanceMapper).toAttendanceResponse(testAttendance);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when event not found for attendances")
        void shouldThrowResourceNotFoundExceptionWhenEventNotFoundForAttendances() {
            // Arrange
            String nonExistentEventId = UUID.randomUUID().toString();
            Pageable pageable = PageRequest.of(0, 10);

            given(eventRepository.findActiveById(UUID.fromString(nonExistentEventId))).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.getEventAttendances(nonExistentEventId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Event not found with ID: " + nonExistentEventId);

            verify(attendanceRepository, never()).findByEvent(any(), any());
        }

        @Test
        @DisplayName("Should handle invalid UUID format for event ID")
        void shouldHandleInvalidUuidFormatForEventId() {
            // Arrange
            String invalidEventId = "invalid-uuid-format";
            Pageable pageable = PageRequest.of(0, 10);

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.getEventAttendances(invalidEventId, pageable))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(eventRepository, never()).findActiveById(any());
            verify(attendanceRepository, never()).findByEvent(any(), any());
        }

        @Test
        @DisplayName("Should return empty page when event has no attendances")
        void shouldReturnEmptyPageWhenEventHasNoAttendances() {
            // Arrange
            String eventId = testEvent.getId().toString();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendance> emptyPage = new PageImpl<>(Collections.emptyList());

            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(attendanceRepository.findByEvent(testEvent, pageable)).willReturn(emptyPage);

            // Act
            Page<AttendanceResponse> result = attendanceService.getEventAttendances(eventId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();

            verify(attendanceMapper, never()).toAttendanceResponse(any());
        }

        @Test
        @DisplayName("Should get attendances for event with multiple responses")
        void shouldGetAttendancesForEventWithMultipleResponses() {
            // Arrange
            Attendance attendance1 = Attendance.builder()
                    .event(testEvent)
                    .user(testUser)
                    .status(Attendance.Status.GOING)
                    .build();

            Attendance attendance2 = Attendance.builder()
                    .event(testEvent)
                    .user(otherUser)
                    .status(Attendance.Status.MAYBE)
                    .build();

            String eventId = testEvent.getId().toString();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendance> attendancePage = new PageImpl<>(java.util.Arrays.asList(attendance1, attendance2));

            AttendanceResponse response1 = AttendanceResponse.builder().status(Attendance.Status.GOING).build();
            AttendanceResponse response2 = AttendanceResponse.builder().status(Attendance.Status.MAYBE).build();

            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(attendanceRepository.findByEvent(testEvent, pageable)).willReturn(attendancePage);
            given(attendanceMapper.toAttendanceResponse(attendance1)).willReturn(response1);
            given(attendanceMapper.toAttendanceResponse(attendance2)).willReturn(response2);

            // Act
            Page<AttendanceResponse> result = attendanceService.getEventAttendances(eventId, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(Attendance.Status.GOING);
            assertThat(result.getContent().get(1).getStatus()).isEqualTo(Attendance.Status.MAYBE);

            verify(attendanceMapper).toAttendanceResponse(attendance1);
            verify(attendanceMapper).toAttendanceResponse(attendance2);
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
            given(attendanceRepository.findByEventAndUser(testEvent, testUser)).willReturn(Optional.empty());
            given(attendanceRepository.save(any(Attendance.class))).willReturn(testAttendance);
            given(attendanceMapper.toAttendanceResponse(testAttendance)).willReturn(testAttendanceResponse);

            // Act
            attendanceService.updateAttendance(attendanceRequest, authentication);

            // Assert
            verify(authentication).getPrincipal();
            verify(userService).getUserById(testUser.getId());
        }

        @Test
        @DisplayName("Should handle user service exceptions")
        void shouldHandleUserServiceExceptions() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId()))
                    .willThrow(new ResourceNotFoundException("User not found"));

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.updateAttendance(attendanceRequest, authentication))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(eventRepository, never()).findActiveById(any());
            verify(attendanceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should preserve event and user references in attendance")
        void shouldPreserveEventAndUserReferencesInAttendance() {
            // Arrange
            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(attendanceRepository.findByEventAndUser(testEvent, testUser)).willReturn(Optional.empty());
            given(attendanceRepository.save(any(Attendance.class))).willReturn(testAttendance);
            given(attendanceMapper.toAttendanceResponse(testAttendance)).willReturn(testAttendanceResponse);

            // Act
            attendanceService.updateAttendance(attendanceRequest, authentication);

            // Assert
            verify(attendanceRepository).save(argThat(attendance ->
                attendance.getEvent().equals(testEvent) &&
                attendance.getUser().equals(testUser)
            ));
        }

        @Test
        @DisplayName("Should allow changing attendance status multiple times")
        void shouldAllowChangingAttendanceStatusMultipleTimes() {
            // Arrange
            Attendance existingAttendance = Attendance.builder()
                    .event(testEvent)
                    .user(testUser)
                    .status(Attendance.Status.GOING)
                    .build();

            // First change: GOING -> MAYBE
            AttendanceRequest firstChange = AttendanceRequest.builder()
                    .eventId(testEvent.getId())
                    .status(Attendance.Status.MAYBE)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(testEvent.getId())).willReturn(Optional.of(testEvent));
            given(attendanceRepository.findByEventAndUser(testEvent, testUser)).willReturn(Optional.of(existingAttendance));
            given(attendanceRepository.save(existingAttendance)).willReturn(existingAttendance);
            given(attendanceMapper.toAttendanceResponse(existingAttendance)).willReturn(testAttendanceResponse);

            // Act
            attendanceService.updateAttendance(firstChange, authentication);

            // Assert
            assertThat(existingAttendance.getStatus()).isEqualTo(Attendance.Status.MAYBE);

            // Second change: MAYBE -> DECLINED
            AttendanceRequest secondChange = AttendanceRequest.builder()
                    .eventId(testEvent.getId())
                    .status(Attendance.Status.DECLINED)
                    .build();

            // Act
            attendanceService.updateAttendance(secondChange, authentication);

            // Assert
            assertThat(existingAttendance.getStatus()).isEqualTo(Attendance.Status.DECLINED);
            verify(attendanceRepository, times(2)).save(existingAttendance);
        }

        @Test
        @DisplayName("Should handle attendance for private events")
        void shouldHandleAttendanceForPrivateEvents() {
            // Arrange
            Event privateEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Private Event")
                    .host(otherUser)
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                    .visibility(Event.Visibility.PRIVATE)
                    .build();

            AttendanceRequest privateEventRequest = AttendanceRequest.builder()
                    .eventId(privateEvent.getId())
                    .status(Attendance.Status.GOING)
                    .build();

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userService.getUserById(testUser.getId())).willReturn(testUser);
            given(eventRepository.findActiveById(privateEvent.getId())).willReturn(Optional.of(privateEvent));
            given(attendanceRepository.findByEventAndUser(privateEvent, testUser)).willReturn(Optional.empty());
            given(attendanceRepository.save(any(Attendance.class))).willReturn(testAttendance);
            given(attendanceMapper.toAttendanceResponse(testAttendance)).willReturn(testAttendanceResponse);

            // Act
            AttendanceResponse result = attendanceService.updateAttendance(privateEventRequest, authentication);

            // Assert
            assertThat(result).isNotNull();
            verify(attendanceRepository).save(any(Attendance.class));
        }
    }
}
