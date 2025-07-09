package com.example.demo.controller;

import com.example.demo.domain.dto.request.CreateEventRequest;
import com.example.demo.domain.dto.request.UpdateEventRequest;
import com.example.demo.domain.dto.response.EventResponse;
import com.example.demo.domain.entity.Event;
import com.example.demo.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request,
                                                    Authentication authentication) {
        EventResponse response = eventService.createEvent(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable UUID eventId,
                                                    @Valid @RequestBody UpdateEventRequest request,
                                                    Authentication authentication) {
        EventResponse response = eventService.updateEvent(eventId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId,
                                           Authentication authentication) {
        eventService.deleteEvent(eventId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<EventResponse> getEvent(@PathVariable UUID eventId,
                                                 Authentication authentication) {
        EventResponse response = eventService.getEvent(eventId, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Page<EventResponse>> getUpcomingEvents(
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        Page<EventResponse> events = eventService.getUpcomingEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEventsWithFilters(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Event.Visibility visibility,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        Page<EventResponse> events = eventService.getEventsWithFilters(location, visibility, startDate, endDate, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/my-hosted")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<EventResponse>> getMyHostedEvents(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        Page<EventResponse> events = eventService.getUserHostedEvents(authentication, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/my-attending")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<EventResponse>> getMyAttendingEvents(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        Page<EventResponse> events = eventService.getUserAttendingEvents(authentication, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<EventResponse>> getMyEvents(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        Page<EventResponse> events = eventService.getUserEvents(authentication, pageable);
        return ResponseEntity.ok(events);
    }
}
