package com.example.demo.controller;

import com.example.demo.domain.dto.request.AttendanceRequest;
import com.example.demo.domain.dto.response.AttendanceResponse;
import com.example.demo.service.AttendanceService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendances")
@RequiredArgsConstructor
@Tag(name = "Attendances", description = "Attendance management endpoints")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "RSVP to an event",
        description = "Create or update attendance status for an event (RSVP functionality).",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Attendance updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid attendance data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @Valid @RequestBody AttendanceRequest request,
            Authentication authentication) {
        AttendanceResponse response = attendanceService.updateAttendance(request, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-attendances")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get my attendances",
        description = "Retrieve all attendance records for the authenticated user.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Attendances retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<AttendanceResponse>> getMyAttendances(
            Authentication authentication,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20, sort = "respondedAt") Pageable pageable) {
        Page<AttendanceResponse> attendances = attendanceService.getUserAttendances(authentication, pageable);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get event attendances",
        description = "Retrieve all attendance records for a specific event. Admin only.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event attendances retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Page<AttendanceResponse>> getEventAttendances(
            @Parameter(description = "Event ID to get attendances for") @PathVariable String eventId,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20, sort = "respondedAt") Pageable pageable) {
        Page<AttendanceResponse> attendances = attendanceService.getEventAttendances(eventId, pageable);
        return ResponseEntity.ok(attendances);
    }
}
