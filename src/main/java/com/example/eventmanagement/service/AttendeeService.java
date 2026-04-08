package com.example.eventmanagement.service;

import com.example.eventmanagement.dto.request.AttendeeRequest;
import com.example.eventmanagement.dto.response.AttendeeResponse;
import com.example.eventmanagement.entity.Attendee;
import com.example.eventmanagement.exception.AttendeeNotFoundException;
import com.example.eventmanagement.exception.DuplicateEmailException;
import com.example.eventmanagement.mapper.AttendeeMapper;
import com.example.eventmanagement.repository.AttendeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendeeService {
    private final AttendeeRepository attendeeRepository;
    private final AttendeeMapper attendeeMapper;

    @Transactional
    public AttendeeResponse createAttendee(AttendeeRequest request) {
        log.debug("Creating attendee: {}", request);

        Optional<Attendee> existing = attendeeRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            throw new DuplicateEmailException("Attendee with email " + request.getEmail() + " already exists");
        }

        Attendee attendee = attendeeMapper.toEntity(request);
        Attendee savedAttendee = attendeeRepository.save(attendee);
        log.info("Attendee created with id: {}", savedAttendee.getId());
        return attendeeMapper.toResponse(savedAttendee);
    }

    @Transactional(readOnly = true)
    public AttendeeResponse getAttendeeById(Long id) {
        log.debug("Fetching attendee with id: {}", id);
        Attendee attendee = attendeeRepository.findById(id)
            .orElseThrow(() -> new AttendeeNotFoundException("Attendee not found with id: " + id));
        return attendeeMapper.toResponse(attendee);
    }

    @Transactional(readOnly = true)
    public List<AttendeeResponse> getAllAttendees() {
        log.debug("Fetching all attendees");
        return attendeeRepository.findAll().stream()
            .map(attendeeMapper::toResponse)
            .toList();
    }

    @Transactional
    public AttendeeResponse updateAttendee(Long id, AttendeeRequest request) {
        log.debug("Updating attendee with id: {}", id);
        Attendee attendee = attendeeRepository.findById(id)
            .orElseThrow(() -> new AttendeeNotFoundException("Attendee not found with id: " + id));

        if (!attendee.getEmail().equals(request.getEmail())) {
            Optional<Attendee> existing = attendeeRepository.findByEmail(request.getEmail());
            if (existing.isPresent()) {
                throw new DuplicateEmailException("Attendee with email " + request.getEmail() + " already exists");
            }
        }

        attendeeMapper.updateEntity(request, attendee);
        Attendee updatedAttendee = attendeeRepository.save(attendee);
        return attendeeMapper.toResponse(updatedAttendee);
    }

    @Transactional
    public void deleteAttendee(Long id) {
        log.debug("Deleting attendee with id: {}", id);
        if (!attendeeRepository.existsById(id)) {
            throw new AttendeeNotFoundException("Attendee not found with id: " + id);
        }
        attendeeRepository.deleteById(id);
    }

    @Transactional
    public List<AttendeeResponse> createAttendeesBulk(List<AttendeeRequest> requests) {
        log.debug("Creating {} attendees with transaction", requests.size());

        long distinctCount = requests.stream()
            .map(AttendeeRequest::getEmail)
            .distinct()
            .count();
        if (distinctCount != requests.size()) {
            throw new IllegalArgumentException("Duplicate email addresses in the request list");
        }

        for (AttendeeRequest request : requests) {
            checkEmailUniqueness(request.getEmail());
        }

        List<Attendee> attendees = requests.stream()
            .map(attendeeMapper::toEntity)
            .toList();

        List<Attendee> saved = attendeeRepository.saveAll(attendees);
        return saved.stream()
            .map(attendeeMapper::toResponse)
            .toList();
    }

    public List<AttendeeResponse> createAttendeesBulkWithoutTransaction(List<AttendeeRequest> requests) {
        log.debug("Creating {} attendees WITHOUT transaction", requests.size());

        List<AttendeeResponse> responses = new ArrayList<>();
        for (AttendeeRequest request : requests) {
            if (attendeeRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new DuplicateEmailException("Email already exists: " + request.getEmail());
            }
            Attendee attendee = attendeeMapper.toEntity(request);
            Attendee saved = attendeeRepository.save(attendee);
            responses.add(attendeeMapper.toResponse(saved));
        }
        return responses;
    }

    private void checkEmailUniqueness(String email) {
        Optional<Attendee> existing = attendeeRepository.findByEmail(email);
        if (existing.isPresent()) {
            throw new DuplicateEmailException("Attendee with email " + email + " already exists");
        }
    }
}
