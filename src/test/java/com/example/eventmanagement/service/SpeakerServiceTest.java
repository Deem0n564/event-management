package com.example.eventmanagement.service;

import com.example.eventmanagement.dto.request.SpeakerRequest;
import com.example.eventmanagement.dto.response.SpeakerResponse;
import com.example.eventmanagement.entity.Speaker;
import com.example.eventmanagement.exception.SpeakerNotFoundException;
import com.example.eventmanagement.mapper.SpeakerMapper;
import com.example.eventmanagement.repository.SpeakerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpeakerServiceTest {

    @Mock
    private SpeakerRepository speakerRepository;

    @Mock
    private SpeakerMapper speakerMapper;

    @InjectMocks
    private SpeakerService speakerService;

    private Speaker speaker;
    private SpeakerRequest request;
    private SpeakerResponse response;

    @BeforeEach
    void setUp() {
        speaker = new Speaker();
        speaker.setId(1L);
        speaker.setFirstName("John");
        speaker.setLastName("Doe");
        speaker.setBio("Java expert");

        request = new SpeakerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBio("Java expert");

        response = new SpeakerResponse();
        response.setId(1L);
        response.setFirstName("John");
        response.setLastName("Doe");
    }

    @Test
    void createSpeaker_Success() {
        when(speakerMapper.toEntity(request)).thenReturn(speaker);
        when(speakerRepository.save(speaker)).thenReturn(speaker);
        when(speakerMapper.toResponse(speaker)).thenReturn(response);

        SpeakerResponse result = speakerService.createSpeaker(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(speakerRepository).save(speaker);
    }

    @Test
    void getSpeakerById_Success() {
        when(speakerRepository.findById(1L)).thenReturn(Optional.of(speaker));
        when(speakerMapper.toResponse(speaker)).thenReturn(response);

        SpeakerResponse result = speakerService.getSpeakerById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getSpeakerById_NotFound_ThrowsException() {
        when(speakerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SpeakerNotFoundException.class, () -> speakerService.getSpeakerById(99L));
    }

    @Test
    void getAllSpeakers_Success() {
        when(speakerRepository.findAll()).thenReturn(List.of(speaker));
        when(speakerMapper.toResponse(speaker)).thenReturn(response);

        List<SpeakerResponse> result = speakerService.getAllSpeakers();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void updateSpeaker_Success() {
        SpeakerRequest updateRequest = new SpeakerRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith");
        updateRequest.setBio("Updated bio");

        when(speakerRepository.findById(1L)).thenReturn(Optional.of(speaker));
        doNothing().when(speakerMapper).updateEntity(updateRequest, speaker);
        when(speakerRepository.save(speaker)).thenReturn(speaker);
        when(speakerMapper.toResponse(speaker)).thenReturn(response);

        SpeakerResponse result = speakerService.updateSpeaker(1L, updateRequest);

        assertNotNull(result);
        verify(speakerMapper).updateEntity(updateRequest, speaker);
        verify(speakerRepository).save(speaker);
    }

    @Test
    void updateSpeaker_NotFound_ThrowsException() {
        when(speakerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SpeakerNotFoundException.class, () -> speakerService.updateSpeaker(99L, request));
    }

    @Test
    void deleteSpeaker_Success() {
        when(speakerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(speakerRepository).deleteById(1L);

        assertDoesNotThrow(() -> speakerService.deleteSpeaker(1L));
        verify(speakerRepository).deleteById(1L);
    }

    @Test
    void deleteSpeaker_NotFound_ThrowsException() {
        when(speakerRepository.existsById(99L)).thenReturn(false);

        assertThrows(SpeakerNotFoundException.class, () -> speakerService.deleteSpeaker(99L));
    }
}