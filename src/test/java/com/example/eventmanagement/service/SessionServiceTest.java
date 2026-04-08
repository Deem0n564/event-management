package com.example.eventmanagement.service;

import com.example.eventmanagement.cache.SessionSearchKey;
import com.example.eventmanagement.dto.projection.SessionFlatDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SpeakerRepository speakerRepository;

    @Mock
    private SessionMapper sessionMapper;

    @InjectMocks
    private SessionService sessionService;

    private Session session;
    private SessionRequest sessionRequest;
    private SessionResponse sessionResponse;
    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(1L);
        event.setName("Test Event");

        session = new Session();
        session.setId(1L);
        session.setTitle("Test Session");
        session.setDescription("Description");
        session.setEvent(event);
        session.setSpeakers(new HashSet<>());

        sessionRequest = new SessionRequest();
        sessionRequest.setTitle("Test Session");
        sessionRequest.setDescription("Description");
        sessionRequest.setEventId(1L);
        sessionRequest.setSpeakerIds(Set.of(1L, 2L));

        sessionResponse = new SessionResponse();
        sessionResponse.setId(1L);
        sessionResponse.setTitle("Test Session");
    }

    @Test
    void createSession_Success() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(sessionMapper.toEntity(sessionRequest)).thenReturn(session);
        when(speakerRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(new Speaker(), new Speaker()));
        when(sessionRepository.save(session)).thenReturn(session);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        SessionResponse result = sessionService.createSession(sessionRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(sessionRepository, times(1)).save(session);
    }

    @Test
    void createSession_EventNotFound_ThrowsException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());
        sessionRequest.setEventId(99L);

        assertThrows(EventNotFoundException.class, () -> sessionService.createSession(sessionRequest));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void getSessionById_Success() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        SessionResponse result = sessionService.getSessionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getSessionById_NotFound_ThrowsException() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class, () -> sessionService.getSessionById(99L));
    }

    @Test
    void getAllSessions_Success() {
        when(sessionRepository.findAll()).thenReturn(List.of(session));
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        List<SessionResponse> result = sessionService.getAllSessions();

        assertEquals(1, result.size());
    }

    @Test
    void getAllSessionsWithSpeakers_Success() {
        when(sessionRepository.findAllWithSpeakersAndEvent()).thenReturn(List.of(session));
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        List<SessionResponse> result = sessionService.getAllSessionsWithSpeakers();

        assertEquals(1, result.size());
        verify(sessionRepository).findAllWithSpeakersAndEvent();
    }

    @Test
    void updateSession_Success() {
        SessionRequest updateRequest = new SessionRequest();
        updateRequest.setTitle("Updated");
        updateRequest.setEventId(1L);
        updateRequest.setSpeakerIds(Set.of(3L));

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionMapper).updateEntity(updateRequest, session);
        when(speakerRepository.findAllById(Set.of(3L))).thenReturn(List.of(new Speaker()));
        when(sessionRepository.save(session)).thenReturn(session);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        SessionResponse result = sessionService.updateSession(1L, updateRequest);

        assertNotNull(result);
        verify(sessionRepository).save(session);
        verify(eventRepository, never()).findById(anyLong());
    }

    @Test
    void updateSession_NotFound_ThrowsException() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class, () -> sessionService.updateSession(99L, sessionRequest));
    }

    @Test
    void deleteSession_Success() {
        when(sessionRepository.existsById(1L)).thenReturn(true);
        doNothing().when(sessionRepository).deleteById(1L);

        assertDoesNotThrow(() -> sessionService.deleteSession(1L));
        verify(sessionRepository).deleteById(1L);
    }

    @Test
    void deleteSession_NotFound_ThrowsException() {
        when(sessionRepository.existsById(99L)).thenReturn(false);

        assertThrows(SessionNotFoundException.class, () -> sessionService.deleteSession(99L));
    }

    @Test
    void searchSessions_CacheMiss() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Session> sessionPage = new PageImpl<>(List.of(session), pageable, 1);
        when(sessionRepository.searchSessions(eq("Иван"), eq("Spring"), eq(pageable))).thenReturn(sessionPage);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        Page<SessionResponse> result = sessionService.searchSessions("Иван", "Spring", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(sessionRepository, times(1)).searchSessions("Иван", "Spring", pageable);
    }

    @Test
    void searchSessions_WithNullParams_CacheMiss() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Session> sessionPage = new PageImpl<>(List.of(session), pageable, 1);
        when(sessionRepository.searchSessions(null, null, pageable)).thenReturn(sessionPage);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        Page<SessionResponse> result = sessionService.searchSessions(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(sessionRepository, times(1)).searchSessions(null, null, pageable);
    }

    @Test
    void searchSessions_WithEmptyStrings() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Session> sessionPage = new PageImpl<>(List.of(session), pageable, 1);
        when(sessionRepository.searchSessions("", "", pageable)).thenReturn(sessionPage);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        Page<SessionResponse> result = sessionService.searchSessions("", "", pageable);

        assertNotNull(result);
        verify(sessionRepository).searchSessions("", "", pageable);
    }

    @Test
    void searchSessions_CacheHit() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Session> sessionPage = new PageImpl<>(List.of(session), pageable, 1);
        when(sessionRepository.searchSessions("Иван", "Spring", pageable)).thenReturn(sessionPage);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
        sessionService.searchSessions("Иван", "Spring", pageable);
        Page<SessionResponse> result = sessionService.searchSessions("Иван", "Spring", pageable);

        assertNotNull(result);
        verify(sessionRepository, times(1)).searchSessions("Иван", "Spring", pageable);
    }

    @Test
    void invalidateCache_AfterCreateSession() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(sessionMapper.toEntity(sessionRequest)).thenReturn(session);
        when(speakerRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(new Speaker(), new Speaker()));
        when(sessionRepository.save(session)).thenReturn(session);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Session> sessionPage = new PageImpl<>(List.of(session), pageable, 1);
        when(sessionRepository.searchSessions(any(), any(), any())).thenReturn(sessionPage);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);
        sessionService.searchSessions("test", "test", pageable);

        sessionService.createSession(sessionRequest);

        when(sessionRepository.searchSessions("test", "test", pageable)).thenReturn(sessionPage);
        Page<SessionResponse> result = sessionService.searchSessions("test", "test", pageable);

        assertNotNull(result);
        verify(sessionRepository, times(2)).searchSessions(any(), any(), any());
    }

    @Test
    void searchSessions_CacheHit_SameParameters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Session> sessionPage = new PageImpl<>(List.of(session), pageable, 1);
        when(sessionRepository.searchSessions("Иван", "Spring", pageable)).thenReturn(sessionPage);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        Page<SessionResponse> result1 = sessionService.searchSessions("Иван", "Spring", pageable);
        Page<SessionResponse> result2 = sessionService.searchSessions("Иван", "Spring", pageable);

        assertNotNull(result2);
        verify(sessionRepository, times(1)).searchSessions("Иван", "Spring", pageable);
    }

    @Test
    void searchSessionsNative_CacheMiss() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Session> sessionPage = new PageImpl<>(List.of(session), pageable, 1);
        when(sessionRepository.searchSessionsNative("Иван", "Spring", pageable)).thenReturn(sessionPage);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        Page<SessionResponse> result = sessionService.searchSessionsNative("Иван", "Spring", pageable);

        assertNotNull(result);
        verify(sessionRepository).searchSessionsNative("Иван", "Spring", pageable);
    }

    @Test
    void searchSessionsNative_CacheHit() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Session> sessionPage = new PageImpl<>(List.of(session), pageable, 1);
        when(sessionRepository.searchSessionsNative("Иван", "Spring", pageable)).thenReturn(sessionPage);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        sessionService.searchSessionsNative("Иван", "Spring", pageable);
        Page<SessionResponse> result = sessionService.searchSessionsNative("Иван", "Spring", pageable);

        assertNotNull(result);
        verify(sessionRepository, times(1)).searchSessionsNative("Иван", "Spring", pageable);
    }

    @Test
    void createSession_WithEmptySpeakerIds() {
        SessionRequest requestWithoutSpeakers = new SessionRequest();
        requestWithoutSpeakers.setTitle("Session without speakers");
        requestWithoutSpeakers.setEventId(1L);
        requestWithoutSpeakers.setSpeakerIds(null); // или пустой Set

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(sessionMapper.toEntity(requestWithoutSpeakers)).thenReturn(session);
        when(sessionRepository.save(session)).thenReturn(session);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        SessionResponse result = sessionService.createSession(requestWithoutSpeakers);

        assertNotNull(result);
        verify(speakerRepository, never()).findAllById(any());
    }

    @Test
    void updateSession_ChangeEventOnly() {
        SessionRequest updateRequest = new SessionRequest();
        updateRequest.setTitle("Updated title");
        updateRequest.setEventId(2L);
        updateRequest.setSpeakerIds(null);

        Event newEvent = new Event();
        newEvent.setId(2L);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionMapper).updateEntity(updateRequest, session);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(newEvent));
        when(sessionRepository.save(session)).thenReturn(session);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        SessionResponse result = sessionService.updateSession(1L, updateRequest);

        assertNotNull(result);
        verify(speakerRepository, never()).findAllById(any());
    }

    @Test
    void updateSession_ChangeSpeakersOnly() {
        SessionRequest updateRequest = new SessionRequest();
        updateRequest.setTitle("Updated title");
        updateRequest.setEventId(1L); // тот же event
        updateRequest.setSpeakerIds(Set.of(5L));

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionMapper).updateEntity(updateRequest, session);
        when(speakerRepository.findAllById(Set.of(5L))).thenReturn(List.of(new Speaker()));
        when(sessionRepository.save(session)).thenReturn(session);
        when(sessionMapper.toResponse(session)).thenReturn(sessionResponse);

        SessionResponse result = sessionService.updateSession(1L, updateRequest);

        assertNotNull(result);
        verify(eventRepository, never()).findById(anyLong());
    }

    @Test
    void searchSessionsNativeOptimized_CacheMiss() {
        Pageable pageable = PageRequest.of(0, 10);

        List<SessionFlatDTO> flatList = List.of(/* создайте мок-объект SessionFlatDTO */);
        when(sessionRepository.searchSessionsFlat("Иван", "Spring", 0, 10)).thenReturn(flatList);
        when(sessionRepository.countSearchSessions("Иван", "Spring")).thenReturn(1L);

        Page<SessionResponse> result = sessionService.searchSessionsNativeOptimized("Иван", "Spring", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(sessionRepository, times(1)).searchSessionsFlat("Иван", "Spring", 0, 10);
        verify(sessionRepository, times(1)).countSearchSessions("Иван", "Spring");
    }

    @Test
    void searchSessionsNativeOptimized_CacheHit() {
        Pageable pageable = PageRequest.of(0, 10);
        List<SessionFlatDTO> flatList = List.of(/* мок */);
        when(sessionRepository.searchSessionsFlat("Иван", "Spring", 0, 10)).thenReturn(flatList);
        when(sessionRepository.countSearchSessions("Иван", "Spring")).thenReturn(1L);

        sessionService.searchSessionsNativeOptimized("Иван", "Spring", pageable);
        Page<SessionResponse> result = sessionService.searchSessionsNativeOptimized("Иван", "Spring", pageable);

        assertNotNull(result);
        verify(sessionRepository, times(1)).searchSessionsFlat("Иван", "Spring", 0, 10);
    }

    @Test
    void searchSessionsNativeOptimized_NullParams() {
        Pageable pageable = PageRequest.of(0, 10);
        List<SessionFlatDTO> flatList = List.of();
        when(sessionRepository.searchSessionsFlat(null, null, 0, 10)).thenReturn(flatList);
        when(sessionRepository.countSearchSessions(null, null)).thenReturn(0L);

        Page<SessionResponse> result = sessionService.searchSessionsNativeOptimized(null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    // ==================== searchSessionsNativeOptimized ====================

    @Test
    void searchSessionsNativeOptimized_WithSpeakers_CoversLoop() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        SessionFlatDTO dto1 = mock(SessionFlatDTO.class);
        when(dto1.getSessionId()).thenReturn(1L);
        when(dto1.getTitle()).thenReturn("Session 1");
        when(dto1.getDescription()).thenReturn("Desc 1");
        when(dto1.getEventId()).thenReturn(100L);
        when(dto1.getEventName()).thenReturn("Event 1");
        when(dto1.getEventDate()).thenReturn(LocalDate.now());
        when(dto1.getEventLocation()).thenReturn("Location 1");
        when(dto1.getSpeakerId()).thenReturn(10L);
        when(dto1.getFirstName()).thenReturn("John");
        when(dto1.getLastName()).thenReturn("Doe");
        when(dto1.getBio()).thenReturn("Bio");

        SessionFlatDTO dto2 = mock(SessionFlatDTO.class);
        when(dto2.getSessionId()).thenReturn(1L); // тот же sessionId
        when(dto2.getSpeakerId()).thenReturn(20L);
        when(dto2.getFirstName()).thenReturn("Jane");
        when(dto2.getLastName()).thenReturn("Smith");
        when(dto2.getBio()).thenReturn("Bio2");

        List<SessionFlatDTO> flatList = Arrays.asList(dto1, dto2);

        when(sessionRepository.searchSessionsFlat("Иван", "Spring", 0, 10)).thenReturn(flatList);
        when(sessionRepository.countSearchSessions("Иван", "Spring")).thenReturn(1L);

        // Act
        Page<SessionResponse> result = sessionService.searchSessionsNativeOptimized("Иван", "Spring", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        SessionResponse sessionResponse = result.getContent().get(0);
        assertEquals(2, sessionResponse.getSpeakers().size());
        verify(sessionRepository).searchSessionsFlat("Иван", "Spring", 0, 10);
        verify(sessionRepository).countSearchSessions("Иван", "Spring");
    }

    @Test
    void searchSessionsNativeOptimized_WithoutSpeakers_CoversNullBranch() {
        Pageable pageable = PageRequest.of(0, 10);

        SessionFlatDTO dto = mock(SessionFlatDTO.class);
        when(dto.getSessionId()).thenReturn(1L);
        when(dto.getTitle()).thenReturn("Session 1");
        when(dto.getDescription()).thenReturn("Desc 1");
        when(dto.getEventId()).thenReturn(100L);
        when(dto.getEventName()).thenReturn("Event 1");
        when(dto.getEventDate()).thenReturn(LocalDate.now());
        when(dto.getEventLocation()).thenReturn("Location 1");
        when(dto.getSpeakerId()).thenReturn(null); // нет спикера

        List<SessionFlatDTO> flatList = List.of(dto);

        when(sessionRepository.searchSessionsFlat("Иван", "Spring", 0, 10)).thenReturn(flatList);
        when(sessionRepository.countSearchSessions("Иван", "Spring")).thenReturn(1L);

        Page<SessionResponse> result = sessionService.searchSessionsNativeOptimized("Иван", "Spring", pageable);

        assertNotNull(result);
        assertTrue(result.getContent().get(0).getSpeakers().isEmpty());
    }
}