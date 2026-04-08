package com.example.eventmanagement.service;

import com.example.eventmanagement.dto.request.TicketRequest;
import com.example.eventmanagement.dto.response.TicketResponse;
import com.example.eventmanagement.entity.Attendee;
import com.example.eventmanagement.entity.Session;
import com.example.eventmanagement.entity.Ticket;
import com.example.eventmanagement.entity.TicketType;
import com.example.eventmanagement.exception.AttendeeNotFoundException;
import com.example.eventmanagement.exception.SessionNotFoundException;
import com.example.eventmanagement.exception.TicketNotFoundException;
import com.example.eventmanagement.mapper.TicketMapper;
import com.example.eventmanagement.repository.AttendeeRepository;
import com.example.eventmanagement.repository.SessionRepository;
import com.example.eventmanagement.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AttendeeRepository attendeeRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketService ticketService;

    private Ticket ticket;
    private TicketRequest request;
    private TicketResponse response;
    private Attendee attendee;
    private Session session;

    @BeforeEach
    void setUp() {
        attendee = new Attendee();
        attendee.setId(1L);
        attendee.setName("John");

        session = new Session();
        session.setId(1L);
        session.setTitle("Test Session");

        ticket = new Ticket();
        ticket.setId(1L);
        ticket.setType(TicketType.VIP);
        ticket.setPrice(BigDecimal.valueOf(5000));
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setAttendee(attendee);
        ticket.setSession(session);

        request = new TicketRequest();
        request.setType(TicketType.VIP);
        request.setPrice(BigDecimal.valueOf(5000));
        request.setAttendeeId(1L);
        request.setSessionId(1L);

        response = new TicketResponse();
        response.setId(1L);
        response.setType(TicketType.VIP);
        response.setPrice(BigDecimal.valueOf(5000));
    }

    @Test
    void createTicket_Success() {
        when(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(ticketMapper.toEntity(request)).thenReturn(ticket);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = ticketService.createTicket(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void createTicket_AttendeeNotFound_ThrowsException() {
        when(attendeeRepository.findById(99L)).thenReturn(Optional.empty());
        request.setAttendeeId(99L);

        assertThrows(AttendeeNotFoundException.class, () -> ticketService.createTicket(request));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void createTicket_SessionNotFound_ThrowsException() {
        when(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee));
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());
        request.setSessionId(99L);

        assertThrows(SessionNotFoundException.class, () -> ticketService.createTicket(request));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void getTicketById_Success() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = ticketService.getTicketById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getTicketById_NotFound_ThrowsException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById(99L));
    }

    @Test
    void getAllTickets_Success() {
        when(ticketRepository.findAll()).thenReturn(List.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        List<TicketResponse> result = ticketService.getAllTickets();

        assertEquals(1, result.size());
    }

    @Test
    void updateTicket_Success() {
        TicketRequest updateRequest = new TicketRequest();
        updateRequest.setType(TicketType.STANDARD);
        updateRequest.setPrice(BigDecimal.valueOf(2500));
        updateRequest.setAttendeeId(2L);
        updateRequest.setSessionId(2L);

        Attendee newAttendee = new Attendee();
        newAttendee.setId(2L);
        Session newSession = new Session();
        newSession.setId(2L);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        doNothing().when(ticketMapper).updateEntity(updateRequest, ticket);
        when(attendeeRepository.findById(2L)).thenReturn(Optional.of(newAttendee));
        when(sessionRepository.findById(2L)).thenReturn(Optional.of(newSession));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = ticketService.updateTicket(1L, updateRequest);

        assertNotNull(result);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void updateTicket_NotFound_ThrowsException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.updateTicket(99L, request));
    }

    @Test
    void deleteTicket_Success() {
        when(ticketRepository.existsById(1L)).thenReturn(true);
        doNothing().when(ticketRepository).deleteById(1L);

        assertDoesNotThrow(() -> ticketService.deleteTicket(1L));
        verify(ticketRepository).deleteById(1L);
    }

    @Test
    void deleteTicket_NotFound_ThrowsException() {
        when(ticketRepository.existsById(99L)).thenReturn(false);

        assertThrows(TicketNotFoundException.class, () -> ticketService.deleteTicket(99L));
    }

    @Test
    void createTicket_WithNullPurchaseDate_ShouldSetCurrentDate() {
        request.setPurchaseDate(null);
        when(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(ticketMapper.toEntity(request)).thenReturn(ticket);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = ticketService.createTicket(request);

        assertNotNull(result);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void updateTicket_WithNewAttendeeAndSession() {
        TicketRequest updateRequest = new TicketRequest();
        updateRequest.setType(TicketType.STANDARD);
        updateRequest.setPrice(BigDecimal.valueOf(3000));
        updateRequest.setAttendeeId(2L);
        updateRequest.setSessionId(2L);

        Attendee newAttendee = new Attendee();
        newAttendee.setId(2L);
        Session newSession = new Session();
        newSession.setId(2L);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        doNothing().when(ticketMapper).updateEntity(updateRequest, ticket);
        when(attendeeRepository.findById(2L)).thenReturn(Optional.of(newAttendee));
        when(sessionRepository.findById(2L)).thenReturn(Optional.of(newSession));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = ticketService.updateTicket(1L, updateRequest);

        assertNotNull(result);
        assertEquals(newAttendee, ticket.getAttendee());
        assertEquals(newSession, ticket.getSession());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void getAllTicketsWithDetails_Success() {
        when(ticketRepository.findAllWithDetails()).thenReturn(List.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        List<TicketResponse> result = ticketService.getAllTicketsWithDetails();

        assertEquals(1, result.size());
        verify(ticketRepository).findAllWithDetails();
    }

    @Test
    void updateTicket_NewAttendeeNotFound_ThrowsException() {
        TicketRequest updateRequest = new TicketRequest();
        updateRequest.setAttendeeId(99L);
        updateRequest.setSessionId(1L);
        updateRequest.setType(TicketType.STANDARD);
        updateRequest.setPrice(BigDecimal.valueOf(1000));

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        doNothing().when(ticketMapper).updateEntity(updateRequest, ticket);
        when(attendeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AttendeeNotFoundException.class, () -> ticketService.updateTicket(1L, updateRequest));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateTicket_NewSessionNotFound_ThrowsException() {
        TicketRequest updateRequest = new TicketRequest();
        updateRequest.setAttendeeId(1L);
        updateRequest.setSessionId(99L);
        updateRequest.setType(TicketType.STANDARD);
        updateRequest.setPrice(BigDecimal.valueOf(1000));

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        doNothing().when(ticketMapper).updateEntity(updateRequest, ticket);
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class, () -> ticketService.updateTicket(1L, updateRequest));
        verify(ticketRepository, never()).save(any());
    }
}