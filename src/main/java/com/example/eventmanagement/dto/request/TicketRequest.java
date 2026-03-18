package com.example.eventmanagement.dto.request;

import com.example.eventmanagement.entity.TicketType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketRequest {
    @NotNull(message = "Ticket type is required")
    private TicketType type;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    private LocalDateTime purchaseDate;

    @NotNull(message = "Attendee ID is required")
    private Long attendeeId;

    @NotNull(message = "Session ID is required")
    private Long sessionId;
}
