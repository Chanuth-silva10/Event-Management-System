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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;
    private final EventMapper eventMapper;

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse createEvent(CreateEventRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        
        if (request.getEndTime().isBefore(request.getStartTime())) {
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

        return eventMapper.toEventResponse(savedEvent);
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

        return eventMapper.toEventResponse(updatedEvent);
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

        return eventMapper.toEventResponse(event);
    }

    @Cacheable(value = "upcomingEvents")
    @Transactional(readOnly = true)
    public Page<EventResponse> getUpcomingEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findUpcomingEvents(LocalDateTime.now(), pageable);
        return events.map(eventMapper::toEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsWithFilters(String location, Event.Visibility visibility,
                                                   LocalDateTime startDate, LocalDateTime endDate,
                                                   Pageable pageable) {
        String visibilityString = visibility != null ? visibility.name() : null;
        Page<Event> events = eventRepository.findEventsWithFilters(location, visibilityString, startDate, endDate, pageable);
        return events.map(eventMapper::toEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUserHostedEvents(Authentication authentication, Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        Page<Event> events = eventRepository.findByHost(currentUser, pageable);
        return events.map(eventMapper::toEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUserAttendingEvents(Authentication authentication, Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        Page<Event> events = eventRepository.findEventsUserIsAttending(currentUser, pageable);
        return events.map(eventMapper::toEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUserEvents(Authentication authentication, Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        Page<Event> events = eventRepository.findEventsUserIsHostingOrAttending(currentUser, pageable);
        return events.map(eventMapper::toEventResponse);
    }

    private Event getEventById(UUID eventId) {
        return eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
    }

    private User getCurrentUser(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userService.getUserById(userPrincipal.getId());
    }
}
