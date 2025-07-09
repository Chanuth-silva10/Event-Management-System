package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_event_start_time", columnList = "start_time"),
    @Index(name = "idx_event_visibility", columnList = "visibility"),
    @Index(name = "idx_event_location", columnList = "location"),
    @Index(name = "idx_event_deleted", columnList = "deleted"),
    @Index(name = "idx_event_host", columnList = "host_id")
})
@SQLDelete(sql = "UPDATE events SET deleted = true WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"attendances"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(length = 500)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Attendance> attendances = new HashSet<>();

    public enum Visibility {
        PUBLIC, PRIVATE
    }

    public boolean isHost(User user) {
        return host != null && host.getId().equals(user.getId());
    }

    public boolean canBeViewedBy(User user) {
        if (visibility == Visibility.PUBLIC) {
            return true;
        }
        return isHost(user) || user.isAdmin();
    }

    public boolean canBeModifiedBy(User user) {
        return isHost(user) || user.isAdmin();
    }

    public long getAttendeeCount() {
        return attendances.stream()
                .filter(attendance -> attendance.getStatus() == Attendance.Status.GOING)
                .count();
    }
}
