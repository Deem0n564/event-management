package com.example.eventmanagement.service;

import com.example.eventmanagement.dto.request.EventRequest;
import com.example.eventmanagement.dto.response.EventResponse;
import com.example.eventmanagement.entity.Event;
import com.example.eventmanagement.exception.EventNotFoundException;
import com.example.eventmanagement.mapper.EventMapper;
import com.example.eventmanagement.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        log.debug("Fetching event with id: {}", id);

        Event event = eventRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Event not found with id: {}", id);
                return new EventNotFoundException("Event not found with id: " + id);
            });

        log.info("Successfully fetched event: {}", event.getName());
        return eventMapper.toResponse(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> searchEvents(String name, LocalDate date) {
        log.debug("Searching events with name: {}, date: {}", name, date);

        List<Event> events;
        if (name != null && !name.isEmpty()) {
            events = eventRepository.findByNameContainingIgnoreCase(name);
        } else if (date != null) {
            events = eventRepository.findByDate(date);
        } else {
            events = eventRepository.findAll();
        }

        log.info("Found {} events", events.size());
        return eventMapper.toResponseList(events);
    }

    @Transactional
    public EventResponse createEvent(EventRequest eventRequest) {
        log.debug("Creating new event: {}", eventRequest);

        Event event = eventMapper.toEntity(eventRequest);
        Event savedEvent = eventRepository.save(event);

        log.info("Event created successfully with id: {}", savedEvent.getId());

        return eventMapper.toResponse(savedEvent);
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
        eventMapper.updateEntity(request, event);
        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toResponse(updatedEvent);
    }

    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new EventNotFoundException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }
}
