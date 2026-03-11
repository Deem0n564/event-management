package com.example.eventmanagement.service;

import com.example.eventmanagement.dto.request.SpeakerRequest;
import com.example.eventmanagement.dto.response.SpeakerResponse;
import com.example.eventmanagement.entity.Speaker;
import com.example.eventmanagement.exception.SpeakerNotFoundException;
import com.example.eventmanagement.mapper.SpeakerMapper;
import com.example.eventmanagement.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeakerService {
    private final SpeakerRepository speakerRepository;
    private final SpeakerMapper speakerMapper;

    @Transactional
    public SpeakerResponse createSpeaker(SpeakerRequest request) {
        log.debug("Creating speaker: {}", request);
        Speaker speaker = speakerMapper.toEntity(request);
        Speaker savedSpeaker = speakerRepository.save(speaker);
        log.info("Speaker created with id: {}", savedSpeaker.getId());
        return speakerMapper.toResponse(savedSpeaker);
    }

    @Transactional(readOnly = true)
    public SpeakerResponse getSpeakerById(Long id) {
        log.debug("Fetching speaker with id: {}", id);
        Speaker speaker = speakerRepository.findById(id)
            .orElseThrow(() -> new SpeakerNotFoundException("Speaker not found with id: " + id));
        return speakerMapper.toResponse(speaker);
    }

    @Transactional(readOnly = true)
    public List<SpeakerResponse> getAllSpeakers() {
        log.debug("Fetching all speakers");
        return speakerRepository.findAll().stream()
            .map(speakerMapper::toResponse)
            .toList();
    }

    @Transactional
    public SpeakerResponse updateSpeaker(Long id, SpeakerRequest request) {
        log.debug("Updating speaker with id: {}", id);
        Speaker speaker = speakerRepository.findById(id)
            .orElseThrow(() -> new SpeakerNotFoundException("Speaker not found with id: " + id));
        speakerMapper.updateEntity(request, speaker);
        Speaker updatedSpeaker = speakerRepository.save(speaker);
        return speakerMapper.toResponse(updatedSpeaker);
    }

    @Transactional
    public void deleteSpeaker(Long id) {
        log.debug("Deleting speaker with id: {}", id);
        if (!speakerRepository.existsById(id)) {
            throw new SpeakerNotFoundException("Speaker not found with id: " + id);
        }
        speakerRepository.deleteById(id);
    }
}
