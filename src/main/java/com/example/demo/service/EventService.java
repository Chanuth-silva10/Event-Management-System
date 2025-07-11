package com.example.demo.service;

import com.example.demo.controller.AttendanceController;
import com.example.demo.controller.EventController;
import com.example.demo.domain.dto.request.CreateEventRequest;
import com.example.demo.domain.dto.request.UpdateEventRequest;
import com.example.demo.domain.dto.response.EventResponse;
import com.example.demo.domain.dto.response.EventStatusResponse;
import com.example.demo.domain.entity.Attendance;
import com.example.demo.domain.entity.Event;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.mapper.EventMapper;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventMapper eventMapper;
    private final UserService userService;

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse createEvent(CreateEventRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        
        if (request.getEndTime().isBefore(request.getStartTime()) || 
            request.getEndTime().isEqual(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .host(currentUser)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .visibility(request.getVisibility())
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event created with ID: {} by user: {}", savedEvent.getId(), currentUser.getId());

        EventResponse response = eventMapper.toEventResponse(savedEvent);
        return addEventHateoasLinks(response);
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse updateEvent(UUID eventId, UpdateEventRequest request, Authentication authentication) {
        Event event = getEventById(eventId);
        User currentUser = getCurrentUser(authentication);

        if (!event.canBeModifiedBy(currentUser)) {
            throw new UnauthorizedException("You don't have permission to update this event");
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            event.setEndTime(request.getEndTime());
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }
        if (request.getVisibility() != null) {
            event.setVisibility(request.getVisibility());
        }

        if (event.getEndTime().isBefore(event.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Event updated with ID: {} by user: {}", updatedEvent.getId(), currentUser.getId());

        EventResponse response = eventMapper.toEventResponse(updatedEvent);
        return addEventHateoasLinks(response);
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public void deleteEvent(UUID eventId, Authentication authentication) {
        Event event = getEventById(eventId);
        User currentUser = getCurrentUser(authentication);

        if (!event.canBeModifiedBy(currentUser)) {
            throw new UnauthorizedException("You don't have permission to delete this event");
        }

        eventRepository.delete(event); // This will use soft delete due to @SQLDelete
        log.info("Event deleted with ID: {} by user: {}", eventId, currentUser.getId());
    }

    @Cacheable(value = "events", key = "#eventId")
    @Transactional(readOnly = true)
    public EventResponse getEvent(UUID eventId, Authentication authentication) {
        Event event = getEventById(eventId);
        User currentUser = getCurrentUser(authentication);

        if (!event.canBeViewedBy(currentUser)) {
            throw new UnauthorizedException("You don't have permission to view this event");
        }

        EventResponse response = eventMapper.toEventResponse(event);
        return addEventHateoasLinks(response);
    }

    @Cacheable(value = "upcomingEvents")
    @Transactional(readOnly = true)
    public Page<EventResponse> getUpcomingEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findUpcomingEvents(LocalDateTime.now(), pageable);
        return events.map(event -> addEventHateoasLinks(eventMapper.toEventResponse(event)));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUserHostedEvents(Authentication authentication, Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        Page<Event> events = eventRepository.findByHost(currentUser, pageable);
        return events.map(event -> addEventHateoasLinks(eventMapper.toEventResponse(event)));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUserAttendingEvents(Authentication authentication, Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        Page<Event> events = eventRepository.findEventsUserIsAttending(currentUser, pageable);
        return events.map(event -> addEventHateoasLinks(eventMapper.toEventResponse(event)));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUserEvents(Authentication authentication, Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        Page<Event> events = eventRepository.findEventsUserIsHostingOrAttending(currentUser, pageable);
        return events.map(event -> addEventHateoasLinks(eventMapper.toEventResponse(event)));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEvents(String location, LocalDateTime startDate, 
                                          LocalDateTime endDate, String visibility, Pageable pageable, 
                                          Authentication authentication) {
        User currentUser = authentication != null ? getCurrentUser(authentication) : null;
        Specification<Event> spec = createEventSpecification(location, startDate, endDate, visibility, currentUser);
        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(event -> addEventHateoasLinks(eventMapper.toEventResponse(event)));
    }

    @Transactional(readOnly = true)
    public EventStatusResponse getEventStatus(UUID eventId, Authentication authentication) {
        Event event = getEventById(eventId);
        User currentUser = getCurrentUser(authentication);

        if (!event.canBeViewedBy(currentUser)) {
            throw new UnauthorizedException("You don't have permission to view this event");
        }

        // Count attendances by status
        long goingCount = attendanceRepository.countByEventAndStatus(event, Attendance.Status.GOING);
        long maybeCount = attendanceRepository.countByEventAndStatus(event, Attendance.Status.MAYBE);
        long declinedCount = attendanceRepository.countByEventAndStatus(event, Attendance.Status.DECLINED);
        long totalAttendees = goingCount + maybeCount + declinedCount;

        // Check user's attendance status
        String userAttendanceStatus = attendanceRepository.findByEventAndUser(event, currentUser)
                .map(attendance -> attendance.getStatus().name())
                .orElse("NOT_RESPONDED");

        // Determine event status
        EventStatusResponse.EventStatus status = determineEventStatus(event);

        return EventStatusResponse.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .status(status)
                .totalAttendees(totalAttendees)
                .goingCount(goingCount)
                .maybeCount(maybeCount)
                .declinedCount(declinedCount)
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .canUserAttend(event.getStartTime().isAfter(LocalDateTime.now()))
                .userAttendanceStatus(userAttendanceStatus)
                .build();
    }

    private Event getEventById(UUID eventId) {
        return eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException("Authentication required");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userService.getUserById(userPrincipal.getId());
    }

    // Add helper method to create event specification for filtering
    private Specification<Event> createEventSpecification(String location, LocalDateTime startDate, 
                                                         LocalDateTime endDate, String visibility, User currentUser) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            
            // Only show non-deleted events
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            
            // Handle visibility based on authentication and user role
            if (currentUser == null) {
                // Unauthenticated users can only see public events
                predicates.add(criteriaBuilder.equal(root.get("visibility"), Event.Visibility.PUBLIC));
            } else if (currentUser.isAdmin()) {
                // Admins can see all events regardless of visibility
                if (visibility != null && !visibility.trim().isEmpty()) {
                    try {
                        Event.Visibility vis = Event.Visibility.valueOf(visibility.toUpperCase());
                        predicates.add(criteriaBuilder.equal(root.get("visibility"), vis));
                    } catch (IllegalArgumentException e) {
                        // Invalid visibility value, ignore filter
                    }
                }
            } else {
                // Regular authenticated users see:
                // 1. All public events OR
                // 2. Private events they host OR  
                // 3. Private events they attend (handled by separate repository queries)
                // For simplicity in filtering, we'll primarily show public events
                // and let specific endpoints handle private event access
                if (visibility != null && !visibility.trim().isEmpty()) {
                    try {
                        Event.Visibility vis = Event.Visibility.valueOf(visibility.toUpperCase());
                        if (vis == Event.Visibility.PRIVATE) {
                            // For private events, only show ones they host
                            predicates.add(criteriaBuilder.and(
                                criteriaBuilder.equal(root.get("visibility"), Event.Visibility.PRIVATE),
                                criteriaBuilder.equal(root.get("host"), currentUser)
                            ));
                        } else {
                            predicates.add(criteriaBuilder.equal(root.get("visibility"), vis));
                        }
                    } catch (IllegalArgumentException e) {
                        predicates.add(criteriaBuilder.equal(root.get("visibility"), Event.Visibility.PUBLIC));
                    }
                } else {
                    // Default: show public events and private events they host
                    predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("visibility"), Event.Visibility.PUBLIC),
                        criteriaBuilder.and(
                            criteriaBuilder.equal(root.get("visibility"), Event.Visibility.PRIVATE),
                            criteriaBuilder.equal(root.get("host"), currentUser)
                        )
                    ));
                }
            }
            
            // Location filter (case-insensitive partial match)
            if (location != null && !location.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("location")), 
                    "%" + location.toLowerCase() + "%"
                ));
            }
            
            // Start date filter (events starting on or after this date)
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), startDate));
            }
            
            // End date filter (events ending on or before this date)
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), endDate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // Add helper method to determine event status
    private EventStatusResponse.EventStatus determineEventStatus(Event event) {
        LocalDateTime now = LocalDateTime.now();
        
        if (event.getEndTime().isBefore(now)) {
            return EventStatusResponse.EventStatus.COMPLETED;
        } else if (event.getStartTime().isBefore(now) && event.getEndTime().isAfter(now)) {
            return EventStatusResponse.EventStatus.ONGOING;
        } else {
            return EventStatusResponse.EventStatus.UPCOMING;
        }
    }

    // Add HATEOAS links to EventResponse
    private EventResponse addEventHateoasLinks(EventResponse eventResponse) {
        try {
            // Add self link
            eventResponse.add(linkTo(methodOn(EventController.class)
                .getEvent(eventResponse.getId(), null)).withSelfRel());
            
            // Add update link
            eventResponse.add(linkTo(methodOn(EventController.class)
                .updateEvent(eventResponse.getId(), null, null)).withRel("update"));
            
            // Add delete link  
            eventResponse.add(linkTo(methodOn(EventController.class)
                .deleteEvent(eventResponse.getId(), null)).withRel("delete"));
            
            // Add status link
            eventResponse.add(linkTo(methodOn(EventController.class)
                .getEventStatus(eventResponse.getId(), null)).withRel("status"));
            
            // Add attendances link
            eventResponse.add(linkTo(methodOn(AttendanceController.class)
                .getEventAttendances(eventResponse.getId().toString(), null)).withRel("attendances"));
            
            return eventResponse;
        } catch (Exception e) {
            log.warn("Failed to add HATEOAS links to event response: {}", e.getMessage());
            return eventResponse;
        }
    }
}
