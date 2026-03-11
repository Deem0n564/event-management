package com.example.eventmanagement.repository;

import com.example.eventmanagement.entity.Attendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
    Optional<Attendee> findByEmail(String email);
    List<Attendee> findByNameContainingIgnoreCase(String name);
}
