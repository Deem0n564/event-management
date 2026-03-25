package com.example.eventmanagement.dto.projection;

import java.time.LocalDate;

public interface SessionFlatDTO {
    Long getSessionId();
    String getTitle();
    String getDescription();
    Long getEventId();
    String getEventName();
    LocalDate getEventDate();
    String getEventLocation();
    Long getSpeakerId();
    String getFirstName();
    String getLastName();
    String getBio();
}
