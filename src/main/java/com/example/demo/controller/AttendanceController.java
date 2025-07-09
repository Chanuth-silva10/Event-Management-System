package com.example.demo.controller;

import com.example.demo.domain.dto.request.AttendanceRequest;
import com.example.demo.domain.dto.response.AttendanceResponse;
import com.example.demo.service.AttendanceService;
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
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<AttendanceResponse> updateAttendance(@Valid @RequestBody AttendanceRequest request,
                                                              Authentication authentication) {
        AttendanceResponse response = attendanceService.updateAttendance(request, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-attendances")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<AttendanceResponse>> getMyAttendances(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "respondedAt") Pageable pageable) {
        Page<AttendanceResponse> attendances = attendanceService.getUserAttendances(authentication, pageable);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AttendanceResponse>> getEventAttendances(
            @PathVariable String eventId,
            @PageableDefault(size = 20, sort = "respondedAt") Pageable pageable) {
        Page<AttendanceResponse> attendances = attendanceService.getEventAttendances(eventId, pageable);
        return ResponseEntity.ok(attendances);
    }
}
