package com.example.demo.controller;

import com.example.demo.domain.dto.request.CreateEventRequest;
import com.example.demo.domain.dto.request.UpdateEventRequest;
import com.example.demo.domain.dto.response.EventResponse;
import com.example.demo.domain.dto.response.EventStatusResponse;
import com.example.demo.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Create a new event",
        description = "Create a new event. Only authenticated users can create events.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Event created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication) {
        EventResponse response = eventService.createEvent(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Update an event",
        description = "Update an existing event. Only the event host or admin can update events.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not event host"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<EventResponse> updateEvent(
            @Parameter(description = "Event ID to update") @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request,
            Authentication authentication) {
        EventResponse response = eventService.updateEvent(eventId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Delete an event",
        description = "Soft delete an event. Only the event host or admin can delete events.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not event host"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "Event ID to delete") @PathVariable UUID eventId,
            Authentication authentication) {
        eventService.deleteEvent(eventId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get event details",
        description = "Retrieve details of a specific event. Private events are only visible to participants and hosts.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event details retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Event not found or not accessible")
    })
    public ResponseEntity<EventResponse> getEvent(
            @Parameter(description = "Event ID to retrieve") @PathVariable UUID eventId,
            Authentication authentication) {
        EventResponse response = eventService.getEvent(eventId, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming")
    @Operation(
        summary = "Get upcoming events",
        description = "Retrieve all public upcoming events. No authentication required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Upcoming events retrieved successfully")
    })
    public ResponseEntity<Page<EventResponse>> getUpcomingEvents(
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        Page<EventResponse> events = eventService.getUpcomingEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/my-hosted")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get my hosted events",
        description = "Retrieve all events hosted by the authenticated user.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hosted events retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<EventResponse>> getMyHostedEvents(
            Authentication authentication,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        Page<EventResponse> events = eventService.getUserHostedEvents(authentication, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/my-attending")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get events I'm attending",
        description = "Retrieve all events the authenticated user is attending.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Attending events retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<EventResponse>> getMyAttendingEvents(
            Authentication authentication,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        Page<EventResponse> events = eventService.getUserAttendingEvents(authentication, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get all my events",
        description = "Retrieve all events related to the authenticated user (both hosted and attending).",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User events retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<EventResponse>> getMyEvents(
            Authentication authentication,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        Page<EventResponse> events = eventService.getUserEvents(authentication, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get event status",
        description = "Get attendance status and event metadata for the authenticated user.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Event not found or not accessible")
    })
    public ResponseEntity<EventStatusResponse> getEventStatus(
            @Parameter(description = "Event ID to get status for") @PathVariable UUID eventId,
            Authentication authentication) {
        EventStatusResponse status = eventService.getEventStatus(eventId, authentication);
        return ResponseEntity.ok(status);
    }

    @GetMapping
    @Operation(
        summary = "Get all events with filters",
        description = "Retrieve events with optional filtering by location, date range, and visibility. Public endpoint."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<Page<EventResponse>> getAllEvents(
            @Parameter(description = "Filter by location (partial match)")
            @RequestParam(required = false) String location,
            @Parameter(description = "Filter events starting from this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter events ending before this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Filter by visibility (PUBLIC, PRIVATE)")
            @RequestParam(required = false) String visibility,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable,
            Authentication authentication) {
        Page<EventResponse> events = eventService.getAllEvents(location, startDate, endDate, visibility, pageable, authentication);
        return ResponseEntity.ok(events);
    }
}
