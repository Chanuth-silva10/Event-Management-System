package com.example.demo.repository;

import com.example.demo.domain.entity.Attendance;
import com.example.demo.domain.entity.Event;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Attendance.AttendanceId> {

    Optional<Attendance> findByEventAndUser(Event event, User user);

    @Query("SELECT a FROM Attendance a WHERE a.user = :user")
    Page<Attendance> findByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT a FROM Attendance a WHERE a.event = :event")
    Page<Attendance> findByEvent(@Param("event") Event event, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.event = :event AND a.status = :status")
    long countByEventAndStatus(@Param("event") Event event, @Param("status") Attendance.Status status);

    boolean existsByEventAndUser(Event event, User user);
}
