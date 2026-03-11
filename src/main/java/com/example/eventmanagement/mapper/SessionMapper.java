package com.example.eventmanagement.mapper;

import com.example.eventmanagement.dto.request.SessionRequest;
import com.example.eventmanagement.dto.response.SessionResponse;
import com.example.eventmanagement.dto.response.SpeakerResponse;
import com.example.eventmanagement.entity.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SessionMapper {
    private final EventMapper eventMapper;
    private final SpeakerMapper speakerMapper;

    public Session toEntity(SessionRequest request) {
        if (request == null) {
            return null;
        }
        Session session = new Session();
        session.setTitle(request.getTitle());
        session.setDescription(request.getDescription());
        return session;
    }

    public SessionResponse toResponse(Session session) {
        if (session == null) {
            return null;
        }
        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setTitle(session.getTitle());
        response.setDescription(session.getDescription());
        response.setEvent(eventMapper.toResponse(session.getEvent()));

        if (session.getSpeakers() != null) {
            Set<SpeakerResponse> speakerResponses = session.getSpeakers().stream()
                .map(speakerMapper::toResponse)
                .collect(Collectors.toSet());
            response.setSpeakers(speakerResponses);
        }
        return response;
    }

    public void updateEntity(SessionRequest request, Session session) {
        if (request == null || session == null) {
            return;
        }
        session.setTitle(request.getTitle());
        session.setDescription(request.getDescription());
    }
}
