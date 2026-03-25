package com.example.eventmanagement.controller;

import com.example.eventmanagement.dto.request.SessionRequest;
import com.example.eventmanagement.dto.response.SessionResponse;
import com.example.eventmanagement.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;

    @GetMapping("/search")
    public Page<SessionResponse> searchSessions(
        @RequestParam(required = false) String speakerFirstName,
        @RequestParam(required = false) String title,
        @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return sessionService.searchSessions(speakerFirstName, title, pageable);
    }

    @GetMapping("/search-native")
    public Page<SessionResponse> searchSessionsNative(
        @RequestParam(required = false) String speakerFirstName,
        @RequestParam(required = false) String title,
        @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return sessionService.searchSessionsNative(speakerFirstName, title, pageable);
    }

    @GetMapping("/search-native-optimized")
    public Page<SessionResponse> searchSessionsNativeOptimized(
        @RequestParam(required = false) String speakerFirstName,
        @RequestParam(required = false) String title,
        @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return sessionService.searchSessionsNativeOptimized(speakerFirstName, title, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(@RequestBody @Valid SessionRequest request) {
        return sessionService.createSession(request);
    }

    @GetMapping("/{id}")
    public SessionResponse getSessionById(@PathVariable Long id) {
        return sessionService.getSessionById(id);
    }

    @GetMapping
    public List<SessionResponse> getAllSessions() {
        return sessionService.getAllSessions();
    }

    @GetMapping("/with-speakers")
    public List<SessionResponse> getAllSessionsWithSpeakers() {
        return sessionService.getAllSessionsWithSpeakers();
    }

    @PutMapping("/{id}")
    public SessionResponse updateSession(@PathVariable Long id, @RequestBody @Valid SessionRequest request) {
        return sessionService.updateSession(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
    }
}
