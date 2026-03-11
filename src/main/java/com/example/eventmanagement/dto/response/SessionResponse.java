package com.example.eventmanagement.dto.response;

import lombok.Data;

import java.util.Set;

@Data
public class SessionResponse {
    private Long id;
    private String title;
    private String description;
    private EventResponse event;
    private Set<SpeakerResponse> speakers;
}
