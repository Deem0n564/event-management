package com.example.eventmanagement.service;

import com.example.eventmanagement.cache.SessionSearchKey;
import com.example.eventmanagement.dto.request.SessionRequest;
import com.example.eventmanagement.dto.response.SessionResponse;
import com.example.eventmanagement.entity.Event;
import com.example.eventmanagement.entity.Session;
import com.example.eventmanagement.entity.Speaker;
import com.example.eventmanagement.exception.EventNotFoundException;
import com.example.eventmanagement.exception.SessionNotFoundException;
import com.example.eventmanagement.mapper.SessionMapper;
import com.example.eventmanagement.repository.EventRepository;
import com.example.eventmanagement.repository.SessionRepository;
import com.example.eventmanagement.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final SpeakerRepository speakerRepository;
    private final SessionMapper sessionMapper;

    private final Map<SessionSearchKey, Page<SessionResponse>> sessionCache = new HashMap<>();

    @Transactional(readOnly = true)
    public Page<SessionResponse> searchSessions(String speakerFirstName, String title, Pageable pageable) {
        SessionSearchKey key = new SessionSearchKey(speakerFirstName, title, pageable.getPageNumber(),
            pageable.getPageSize(), pageable.getSort().toString());

        if (sessionCache.containsKey(key)) {
            log.debug("Cache hit for key: {}", key);
            return sessionCache.get(key);
        }

        log.debug("Cache miss for key: {}, executing DB query", key);
        Page<Session> sessionsPage = sessionRepository.searchSessions(speakerFirstName, title, pageable);
        Page<SessionResponse> responsePage = sessionsPage.map(sessionMapper::toResponse);

        sessionCache.put(key, responsePage);
        return responsePage;
    }

    @Transactional(readOnly = true)
    public Page<SessionResponse> searchSessionsNative(String speakerFirstName, String title, Pageable pageable) {
        SessionSearchKey key = new SessionSearchKey(
            speakerFirstName,
            title,
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().toString() + "_native"
        );

        if (sessionCache.containsKey(key)) {
            log.debug("Cache hit (native) for key: {}", key);
            return sessionCache.get(key);
        }

        log.debug("Cache miss (native) for key: {}", key);
        Page<Session> sessionsPage = sessionRepository.searchSessionsNative(speakerFirstName, title, pageable);
        Page<SessionResponse> responsePage = sessionsPage.map(sessionMapper::toResponse);

        sessionCache.put(key, responsePage);
        return responsePage;
    }

    @Transactional
    public SessionResponse createSession(SessionRequest request) {
        log.debug("Creating session: {}", request);
        Event event = eventRepository.findById(request.getEventId())
            .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + request.getEventId()));

        Session session = sessionMapper.toEntity(request);
        session.setEvent(event);

        if (request.getSpeakerIds() != null && !request.getSpeakerIds().isEmpty()) {
            Set<Speaker> speakers = new HashSet<>(speakerRepository.findAllById(request.getSpeakerIds()));
            session.setSpeakers(speakers);
        }

        Session savedSession = sessionRepository.save(session);
        log.info("Session created with id: {}", savedSession.getId());

        invalidateCache();

        return sessionMapper.toResponse(savedSession);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionById(Long id) {
        log.debug("Fetching session with id: {}", id);
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + id));
        return sessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getAllSessions() {
        log.debug("Fetching all sessions without optimization (N+1 problem)");
        List<Session> sessions = sessionRepository.findAll();
        return sessions.stream().map(sessionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getAllSessionsWithSpeakers() {
        log.debug("Fetching all sessions with speakers using @EntityGraph (solves N+1)");
        List<Session> sessions = sessionRepository.findAllWithSpeakersAndEvent();
        return sessions.stream().map(sessionMapper::toResponse).toList();
    }

    @Transactional
    public SessionResponse updateSession(Long id, SessionRequest request) {
        log.debug("Updating session with id: {}", id);
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + id));

        sessionMapper.updateEntity(request, session);

        if (!session.getEvent().getId().equals(request.getEventId())) {
            Event newEvent = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + request.getEventId()));
            session.setEvent(newEvent);
        }

        if (request.getSpeakerIds() != null) {
            Set<Speaker> speakers = new HashSet<>(speakerRepository.findAllById(request.getSpeakerIds()));
            session.setSpeakers(speakers);
        }

        Session updatedSession = sessionRepository.save(session);

        invalidateCache();

        return sessionMapper.toResponse(updatedSession);
    }

    @Transactional
    public void deleteSession(Long id) {
        log.debug("Deleting session with id: {}", id);
        if (!sessionRepository.existsById(id)) {
            throw new SessionNotFoundException("Session not found with id: " + id);
        }
        sessionRepository.deleteById(id);

        invalidateCache();
    }

    private void invalidateCache() {
        log.debug("Invalidating session cache");
        sessionCache.clear();
    }
}
