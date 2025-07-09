package com.example.demo.domain.dto.request;

import com.example.demo.domain.entity.Attendance;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Status is required")
    private Attendance.Status status;
}
