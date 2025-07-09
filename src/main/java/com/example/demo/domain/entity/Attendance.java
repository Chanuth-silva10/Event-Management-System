package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendances", indexes = {
    @Index(name = "idx_attendance_event", columnList = "event_id"),
    @Index(name = "idx_attendance_user", columnList = "user_id"),
    @Index(name = "idx_attendance_status", columnList = "status")
})
@IdClass(Attendance.AttendanceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Attendance {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @EqualsAndHashCode.Include
    private Event event;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @EqualsAndHashCode.Include
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.GOING;

    @CreationTimestamp
    @Column(name = "responded_at", nullable = false, updatable = false)
    private LocalDateTime respondedAt;

    public enum Status {
        GOING, MAYBE, DECLINED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceId implements Serializable {
        private UUID event;
        private UUID user;
    }
}
