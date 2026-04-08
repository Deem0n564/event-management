package com.example.eventmanagement.service;

import com.example.eventmanagement.dto.request.AttendeeRequest;
import com.example.eventmanagement.dto.response.AttendeeResponse;
import com.example.eventmanagement.entity.Attendee;
import com.example.eventmanagement.exception.AttendeeNotFoundException;
import com.example.eventmanagement.exception.DuplicateEmailException;
import com.example.eventmanagement.mapper.AttendeeMapper;
import com.example.eventmanagement.repository.AttendeeRepository;
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
class AttendeeServiceTest {

    @Mock
    private AttendeeRepository attendeeRepository;

    @Mock
    private AttendeeMapper attendeeMapper;

    @InjectMocks
    private AttendeeService attendeeService;

    private Attendee attendee;
    private AttendeeRequest request;
    private AttendeeResponse response;

    @BeforeEach
    void setUp() {
        attendee = new Attendee();
        attendee.setId(1L);
        attendee.setName("John Doe");
        attendee.setEmail("john@example.com");

        request = new AttendeeRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");

        response = new AttendeeResponse();
        response.setId(1L);
        response.setName("John Doe");
        response.setEmail("john@example.com");
    }

    @Test
    void createAttendee_Success() {
        when(attendeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(attendeeMapper.toEntity(request)).thenReturn(attendee);
        when(attendeeRepository.save(any(Attendee.class))).thenReturn(attendee);
        when(attendeeMapper.toResponse(attendee)).thenReturn(response);

        AttendeeResponse result = attendeeService.createAttendee(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(attendeeRepository, times(1)).save(attendee);
    }

    @Test
    void createAttendee_DuplicateEmail_ThrowsException() {
        when(attendeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(attendee));

        assertThrows(DuplicateEmailException.class, () -> attendeeService.createAttendee(request));
        verify(attendeeRepository, never()).save(any());
    }

    @Test
    void getAttendeeById_Success() {
        when(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee));
        when(attendeeMapper.toResponse(attendee)).thenReturn(response);

        AttendeeResponse result = attendeeService.getAttendeeById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAttendeeById_NotFound_ThrowsException() {
        when(attendeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AttendeeNotFoundException.class, () -> attendeeService.getAttendeeById(99L));
    }

    @Test
    void getAllAttendees_Success() {
        when(attendeeRepository.findAll()).thenReturn(List.of(attendee));
        when(attendeeMapper.toResponse(attendee)).thenReturn(response);

        List<AttendeeResponse> result = attendeeService.getAllAttendees();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void updateAttendee_Success() {
        AttendeeRequest updateRequest = new AttendeeRequest();
        updateRequest.setName("John Updated");
        updateRequest.setEmail("john.new@example.com");

        when(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee));
        when(attendeeRepository.findByEmail("john.new@example.com")).thenReturn(Optional.empty());
        when(attendeeRepository.save(any(Attendee.class))).thenReturn(attendee);
        when(attendeeMapper.toResponse(attendee)).thenReturn(response);

        AttendeeResponse result = attendeeService.updateAttendee(1L, updateRequest);

        assertNotNull(result);
        verify(attendeeMapper).updateEntity(updateRequest, attendee);
        verify(attendeeRepository).save(attendee);
    }

    @Test
    void updateAttendee_DuplicateEmail_ThrowsException() {
        AttendeeRequest updateRequest = new AttendeeRequest();
        updateRequest.setName("John Updated");
        updateRequest.setEmail("existing@example.com");

        when(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee));
        when(attendeeRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new Attendee()));

        assertThrows(DuplicateEmailException.class, () -> attendeeService.updateAttendee(1L, updateRequest));
    }

    @Test
    void deleteAttendee_Success() {
        when(attendeeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(attendeeRepository).deleteById(1L);

        assertDoesNotThrow(() -> attendeeService.deleteAttendee(1L));
        verify(attendeeRepository).deleteById(1L);
    }

    @Test
    void deleteAttendee_NotFound_ThrowsException() {
        when(attendeeRepository.existsById(99L)).thenReturn(false);

        assertThrows(AttendeeNotFoundException.class, () -> attendeeService.deleteAttendee(99L));
    }

    @Test
    void createAttendeesBulk_Success() {
        AttendeeRequest req1 = new AttendeeRequest();
        req1.setName("User1");
        req1.setEmail("user1@example.com");
        AttendeeRequest req2 = new AttendeeRequest();
        req2.setName("User2");
        req2.setEmail("user2@example.com");
        List<AttendeeRequest> requests = List.of(req1, req2);

        Attendee att1 = new Attendee(); att1.setId(1L);
        Attendee att2 = new Attendee(); att2.setId(2L);

        when(attendeeRepository.findByEmail("user1@example.com")).thenReturn(Optional.empty());
        when(attendeeRepository.findByEmail("user2@example.com")).thenReturn(Optional.empty());
        when(attendeeMapper.toEntity(req1)).thenReturn(att1);
        when(attendeeMapper.toEntity(req2)).thenReturn(att2);
        when(attendeeRepository.saveAll(anyList())).thenReturn(List.of(att1, att2));
        when(attendeeMapper.toResponse(att1)).thenReturn(new AttendeeResponse());
        when(attendeeMapper.toResponse(att2)).thenReturn(new AttendeeResponse());

        List<AttendeeResponse> result = attendeeService.createAttendeesBulk(requests);

        assertEquals(2, result.size());
        verify(attendeeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void createAttendeesBulk_DuplicateEmailInList_ThrowsException() {
        AttendeeRequest req1 = new AttendeeRequest();
        req1.setEmail("dup@example.com");
        AttendeeRequest req2 = new AttendeeRequest();
        req2.setEmail("dup@example.com");
        List<AttendeeRequest> requests = List.of(req1, req2);

        assertThrows(IllegalArgumentException.class, () -> attendeeService.createAttendeesBulk(requests));
        verify(attendeeRepository, never()).saveAll(any());
    }

    @Test
    void createAttendeesBulk_ExistingEmail_ThrowsException() {
        AttendeeRequest req1 = new AttendeeRequest();
        req1.setEmail("existing@example.com");
        List<AttendeeRequest> requests = List.of(req1);

        when(attendeeRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new Attendee()));

        assertThrows(DuplicateEmailException.class, () -> attendeeService.createAttendeesBulk(requests));
        verify(attendeeRepository, never()).saveAll(any());
    }

    @Test
    void createAttendeesBulkWithoutTransaction_PartialSuccess() {
        AttendeeRequest req1 = new AttendeeRequest();
        req1.setEmail("ok@example.com");
        AttendeeRequest req2 = new AttendeeRequest();
        req2.setEmail("duplicate@example.com");
        List<AttendeeRequest> requests = List.of(req1, req2);

        Attendee att1 = new Attendee(); att1.setId(1L);

        when(attendeeRepository.findByEmail("ok@example.com")).thenReturn(Optional.empty());
        when(attendeeRepository.findByEmail("duplicate@example.com")).thenReturn(Optional.of(new Attendee()));
        when(attendeeMapper.toEntity(req1)).thenReturn(att1);
        when(attendeeRepository.save(att1)).thenReturn(att1);
        when(attendeeMapper.toResponse(att1)).thenReturn(new AttendeeResponse());

        assertThrows(DuplicateEmailException.class, () -> attendeeService.createAttendeesBulkWithoutTransaction(requests));
        verify(attendeeRepository, times(1)).save(att1);
    }
}
