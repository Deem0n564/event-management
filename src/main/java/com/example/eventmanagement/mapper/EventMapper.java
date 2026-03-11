package com.example.eventmanagement.mapper;

import com.example.eventmanagement.dto.request.EventRequest;
import com.example.eventmanagement.dto.response.EventResponse;
import com.example.eventmanagement.entity.Event;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class EventMapper {

    public Event toEntity(EventRequest request) {
        if (request == null) {
            return null;
        }
        Event event = new Event();
        event.setName(request.getName());
        event.setDate(request.getDate());
        event.setLocation(request.getLocation());
        return event;
    }

    public EventResponse toResponse(Event event) {
        if (event == null) {
            return null;
        }
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setDate(event.getDate());
        response.setLocation(event.getLocation());
        return response;
    }

    public List<EventResponse> toResponseList(List<Event> events) {
        if (events == null) {
            return Collections.emptyList();
        }
        return events.stream()
            .map(this::toResponse)
            .toList();
    }

    public void updateEntity(EventRequest request, Event event) {
        if (request == null || event == null) {
            return;
        }
        event.setName(request.getName());
        event.setDate(request.getDate());
        event.setLocation(request.getLocation());
    }
}
