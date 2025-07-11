package com.example.demo.repository;

import com.example.demo.domain.entity.Event;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    @Query("SELECT e FROM Event e WHERE e.deleted = false")
    Page<Event> findAllActive(Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.deleted = false AND e.id = :id")
    Optional<Event> findActiveById(@Param("id") UUID id);

    @Query("SELECT e FROM Event e WHERE e.deleted = false AND e.startTime >= :startTime")
    Page<Event> findUpcomingEvents(@Param("startTime") LocalDateTime startTime, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.deleted = false AND e.host = :host")
    Page<Event> findByHost(@Param("host") User host, Pageable pageable);

    @Query("SELECT e FROM Event e JOIN e.attendances a WHERE e.deleted = false AND a.user = :user")
    Page<Event> findEventsUserIsAttending(@Param("user") User user, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.deleted = false AND " +
           "(e.host = :user OR e IN (SELECT a.event FROM Attendance a WHERE a.user = :user))")
    Page<Event> findEventsUserIsHostingOrAttending(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.deleted = false AND e.visibility = :visibility")
    long countByVisibility(@Param("visibility") Event.Visibility visibility);
}
