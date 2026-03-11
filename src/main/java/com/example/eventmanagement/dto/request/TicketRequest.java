package com.example.eventmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketRequest {
    @NotBlank(message = "Ticket type is required")
    @Size(min = 2, max = 50, message = "Ticket type must be between 2 and 50 characters")
    private String type;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    private LocalDateTime purchaseDate;

    @NotNull(message = "Attendee ID is required")
    private Long attendeeId;

    @NotNull(message = "Session ID is required")
    private Long sessionId;
}
