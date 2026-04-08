package com.example.eventmanagement.controller;

import com.example.eventmanagement.dto.request.AttendeeRequest;
import com.example.eventmanagement.dto.response.AttendeeResponse;
import com.example.eventmanagement.service.AttendeeService;
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
@RequestMapping("/api/attendees")
@RequiredArgsConstructor
public class AttendeeController {
    private final AttendeeService attendeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttendeeResponse createAttendee(@RequestBody @Valid AttendeeRequest request) {
        return attendeeService.createAttendee(request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AttendeeResponse> createAttendeesBulk(@RequestBody @Valid List<AttendeeRequest> requests) {
        return attendeeService.createAttendeesBulk(requests);
    }

    @PostMapping("/bulk-without-tx")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AttendeeResponse> createAttendeesBulkWithoutTransaction(
        @RequestBody @Valid List<AttendeeRequest> requests) {
        return attendeeService.createAttendeesBulkWithoutTransaction(requests);
    }

    @GetMapping("/{id}")
    public AttendeeResponse getAttendeeById(@PathVariable Long id) {
        return attendeeService.getAttendeeById(id);
    }

    @GetMapping
    public List<AttendeeResponse> getAllAttendees() {
        return attendeeService.getAllAttendees();
    }

    @PutMapping("/{id}")
    public AttendeeResponse updateAttendee(@PathVariable Long id, @RequestBody @Valid AttendeeRequest request) {
        return attendeeService.updateAttendee(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttendee(@PathVariable Long id) {
        attendeeService.deleteAttendee(id);
    }
}
