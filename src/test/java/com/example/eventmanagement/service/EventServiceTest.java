package com.example.eventmanagement.service;

import com.example.eventmanagement.dto.request.EventRequest;
import com.example.eventmanagement.dto.request.SessionRequest;
import com.example.eventmanagement.dto.response.EventResponse;
import com.example.eventmanagement.entity.Event;
import com.example.eventmanagement.entity.Session;
import com.example.eventmanagement.entity.Speaker;
import com.example.eventmanagement.exception.EventNotFoundException;
import com.example.eventmanagement.mapper.EventMapper;
import com.example.eventmanagement.mapper.SessionMapper;
import com.example.eventmanagement.repository.EventRepository;
import com.example.eventmanagement.repository.SessionRepository;
import com.example.eventmanagement.repository.SpeakerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SpeakerRepository speakerRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private SessionMapper sessionMapper;

    @InjectMocks
    private EventService eventService;

    private Event event;
    private EventRequest eventRequest;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(1L);
        event.setName("Java Day");
        event.setDate(LocalDate.of(2026, 6, 15));
        event.setLocation("Moscow");

        eventRequest = new EventRequest();
        eventRequest.setName("Java Day");
        eventRequest.setDate(LocalDate.of(2026, 6, 15));
        eventRequest.setLocation("Moscow");

        eventResponse = new EventResponse();
        eventResponse.setId(1L);
        eventResponse.setName("Java Day");
        eventResponse.setDate(LocalDate.of(2026, 6, 15));
        eventResponse.setLocation("Moscow");
    }

    @Test
    void createEvent_Success() {
        when(eventMapper.toEntity(eventRequest)).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(eventResponse);

        EventResponse result = eventService.createEvent(eventRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Java Day", result.getName());
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void getEventById_Success() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(eventResponse);

        EventResponse result = eventService.getEventById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getEventById_NotFound_ThrowsException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventService.getEventById(99L));
        verify(eventMapper, never()).toResponse(any());
    }

    @Test
    void searchEvents_ByName() {
        List<Event> events = Collections.singletonList(event);
        when(eventRepository.findByNameContainingIgnoreCase("Java")).thenReturn(events);
        when(eventMapper.toResponseList(events)).thenReturn(Collections.singletonList(eventResponse));

        List<EventResponse> result = eventService.searchEvents("Java", null);

        assertEquals(1, result.size());
        assertEquals("Java Day", result.get(0).getName());
        verify(eventRepository).findByNameContainingIgnoreCase("Java");
        verify(eventRepository, never()).findByDate(any());
    }

    @Test
    void searchEvents_ByDate() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        List<Event> events = Collections.singletonList(event);
        when(eventRepository.findByDate(date)).thenReturn(events);
        when(eventMapper.toResponseList(events)).thenReturn(Collections.singletonList(eventResponse));

        List<EventResponse> result = eventService.searchEvents(null, date);

        assertEquals(1, result.size());
        verify(eventRepository).findByDate(date);
    }

    @Test
    void searchEvents_NoParams_ReturnsAll() {
        List<Event> events = Collections.singletonList(event);
        when(eventRepository.findAll()).thenReturn(events);
        when(eventMapper.toResponseList(events)).thenReturn(Collections.singletonList(eventResponse));

        List<EventResponse> result = eventService.searchEvents(null, null);

        assertEquals(1, result.size());
        verify(eventRepository).findAll();
    }

    @Test
    void updateEvent_Success() {
        EventRequest updateRequest = new EventRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setDate(LocalDate.of(2026, 7, 20));
        updateRequest.setLocation("SPB");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        doNothing().when(eventMapper).updateEntity(updateRequest, event);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(eventResponse);

        EventResponse result = eventService.updateEvent(1L, updateRequest);

        assertNotNull(result);
        verify(eventMapper).updateEntity(updateRequest, event);
        verify(eventRepository).save(event);
    }

    @Test
    void updateEvent_NotFound_ThrowsException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventService.updateEvent(99L, eventRequest));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void deleteEvent_Success() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        doNothing().when(eventRepository).deleteById(1L);

        assertDoesNotThrow(() -> eventService.deleteEvent(1L));
        verify(eventRepository).deleteById(1L);
    }

    @Test
    void deleteEvent_NotFound_ThrowsException() {
        when(eventRepository.existsById(99L)).thenReturn(false);

        assertThrows(EventNotFoundException.class, () -> eventService.deleteEvent(99L));
        verify(eventRepository, never()).deleteById(any());
    }
}