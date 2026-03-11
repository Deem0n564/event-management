package com.example.eventmanagement.repository;

import com.example.eventmanagement.entity.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByAttendeeId(Long attendeeId);
    List<Ticket> findBySessionId(Long sessionId);

    @EntityGraph(attributePaths = {"attendee", "session", "session.event", "session.speakers"})
    List<Ticket> findAll();

    @Query("SELECT t FROM Ticket t " +
        "LEFT JOIN FETCH t.attendee " +
        "LEFT JOIN FETCH t.session s " +
        "LEFT JOIN FETCH s.event " +
        "LEFT JOIN FETCH s.speakers")
    List<Ticket> findAllWithDetails();
}
