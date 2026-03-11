package com.example.eventmanagement.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketResponse {
    private Long id;
    private String type;
    private BigDecimal price;
    private LocalDateTime purchaseDate;
    private AttendeeResponse attendee;
    private SessionResponse session;
}
