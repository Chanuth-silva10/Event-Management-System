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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;
    private final UserService userService;
    private final AttendanceMapper attendanceMapper;

    @Transactional
    public AttendanceResponse updateAttendance(AttendanceRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Event event = eventRepository.findActiveById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + request.getEventId()));

        Attendance attendance = attendanceRepository.findByEventAndUser(event, currentUser)
                .orElse(Attendance.builder()
                        .event(event)
                        .user(currentUser)
                        .build());

        attendance.setStatus(request.getStatus());
        Attendance savedAttendance = attendanceRepository.save(attendance);

        log.info("Attendance updated for user: {} and event: {} with status: {}",
                currentUser.getId(), event.getId(), request.getStatus());

        return attendanceMapper.toAttendanceResponse(savedAttendance);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getUserAttendances(Authentication authentication, Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        Page<Attendance> attendances = attendanceRepository.findByUser(currentUser, pageable);
        return attendances.map(attendanceMapper::toAttendanceResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getEventAttendances(String eventId, Pageable pageable) {
        Event event = eventRepository.findActiveById(UUID.fromString(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        
        Page<Attendance> attendances = attendanceRepository.findByEvent(event, pageable);
        return attendances.map(attendanceMapper::toAttendanceResponse);
    }

    private User getCurrentUser(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userService.getUserById(userPrincipal.getId());
    }
}
