package com.example.eventmanagement.mapper;

import com.example.eventmanagement.dto.request.TicketRequest;
import com.example.eventmanagement.dto.response.TicketResponse;
import com.example.eventmanagement.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketMapper {
    private final AttendeeMapper attendeeMapper;
    private final SessionMapper sessionMapper;

    public Ticket toEntity(TicketRequest request) {
        if (request == null) {
            return null;
        }
        Ticket ticket = new Ticket();
        ticket.setType(request.getType());
        ticket.setPrice(request.getPrice());
        ticket.setPurchaseDate(request.getPurchaseDate());
        return ticket;
    }

    public TicketResponse toResponse(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setType(ticket.getType());
        response.setPrice(ticket.getPrice());
        response.setPurchaseDate(ticket.getPurchaseDate());
        response.setAttendee(attendeeMapper.toResponse(ticket.getAttendee()));
        response.setSession(sessionMapper.toResponse(ticket.getSession()));
        return response;
    }

    public void updateEntity(TicketRequest request, Ticket ticket) {
        if (request == null || ticket == null) {
            return;
        }
        ticket.setType(request.getType());
        ticket.setPrice(request.getPrice());
        if (request.getPurchaseDate() != null) {
            ticket.setPurchaseDate(request.getPurchaseDate());
        }
    }
}
