package com.example.eventmanagement.repository;

import com.example.eventmanagement.entity.Session;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByEventId(Long eventId);

    @EntityGraph(attributePaths = {"speakers", "event"})
    List<Session> findAll();

    @Query("SELECT DISTINCT s FROM Session s LEFT JOIN FETCH s.speakers LEFT JOIN FETCH s.event")
    List<Session> findAllWithSpeakersAndEvent();

    @EntityGraph(attributePaths = {"speakers", "event"})
    Optional<Session> findById(Long id);
}
