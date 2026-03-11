package com.example.eventmanagement.controller;

import com.example.eventmanagement.dto.request.SpeakerRequest;
import com.example.eventmanagement.dto.response.SpeakerResponse;
import com.example.eventmanagement.service.SpeakerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/speakers")
@RequiredArgsConstructor
public class SpeakerController {
    private final SpeakerService speakerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpeakerResponse createSpeaker(@RequestBody @Valid SpeakerRequest request) {
        return speakerService.createSpeaker(request);
    }

    @GetMapping("/{id}")
    public SpeakerResponse getSpeakerById(@PathVariable Long id) {
        return speakerService.getSpeakerById(id);
    }

    @GetMapping
    public List<SpeakerResponse> getAllSpeakers() {
        return speakerService.getAllSpeakers();
    }

    @PutMapping("/{id}")
    public SpeakerResponse updateSpeaker(@PathVariable Long id, @RequestBody @Valid SpeakerRequest request) {
        return speakerService.updateSpeaker(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSpeaker(@PathVariable Long id) {
        speakerService.deleteSpeaker(id);
    }
}
