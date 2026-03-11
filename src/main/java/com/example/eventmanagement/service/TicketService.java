package com.example.eventmanagement.service;

import com.example.eventmanagement.dto.request.TicketRequest;
import com.example.eventmanagement.dto.response.TicketResponse;
import com.example.eventmanagement.entity.Attendee;
import com.example.eventmanagement.entity.Session;
import com.example.eventmanagement.entity.Ticket;
import com.example.eventmanagement.exception.AttendeeNotFoundException;
import com.example.eventmanagement.exception.SessionNotFoundException;
import com.example.eventmanagement.exception.TicketNotFoundException;
import com.example.eventmanagement.mapper.TicketMapper;
import com.example.eventmanagement.repository.AttendeeRepository;
import com.example.eventmanagement.repository.SessionRepository;
import com.example.eventmanagement.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final AttendeeRepository attendeeRepository;
    private final SessionRepository sessionRepository;
    private final TicketMapper ticketMapper;

    @Transactional
    public TicketResponse createTicket(TicketRequest request) {
        log.debug("Creating ticket: {}", request);

        Attendee attendee = attendeeRepository.findById(request.getAttendeeId())
            .orElseThrow(() -> new AttendeeNotFoundException("Attendee not found with id: " + request.getAttendeeId()));

        Session session = sessionRepository.findById(request.getSessionId())
            .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + request.getSessionId()));

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setAttendee(attendee);
        ticket.setSession(session);

        if (ticket.getPurchaseDate() == null) {
            ticket.setPurchaseDate(LocalDateTime.now());
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created with id: {}", savedTicket.getId());
        return ticketMapper.toResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        log.debug("Fetching ticket with id: {}", id);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
        return ticketMapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        log.debug("Fetching all tickets without optimization (N+1)");
        List<Ticket> tickets = ticketRepository.findAll(); // Без @EntityGraph будет много запросов
        return tickets.stream().map(ticketMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTicketsWithDetails() {
        log.debug("Fetching all tickets with details using @EntityGraph (solves N+1)");
        List<Ticket> tickets = ticketRepository.findAllWithDetails();
        return tickets.stream().map(ticketMapper::toResponse).toList();
    }

    @Transactional
    public TicketResponse updateTicket(Long id, TicketRequest request) {
        log.debug("Updating ticket with id: {}", id);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        ticketMapper.updateEntity(request, ticket);

        if (!ticket.getAttendee().getId().equals(request.getAttendeeId())) {
            Attendee newAttendee = attendeeRepository.findById(request.getAttendeeId())
                .orElseThrow(
                    () -> new AttendeeNotFoundException("Attendee not found with id: " + request.getAttendeeId()));
            ticket.setAttendee(newAttendee);
        }

        if (!ticket.getSession().getId().equals(request.getSessionId())) {
            Session newSession = sessionRepository.findById(request.getSessionId())
                .orElseThrow(
                    () -> new SessionNotFoundException("Session not found with id: " + request.getSessionId()));
            ticket.setSession(newSession);
        }

        Ticket updatedTicket = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updatedTicket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        log.debug("Deleting ticket with id: {}", id);
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException("Ticket not found with id: " + id);
        }
        ticketRepository.deleteById(id);
    }
}
