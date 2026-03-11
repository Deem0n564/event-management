package com.example.eventmanagement.mapper;

import com.example.eventmanagement.dto.request.AttendeeRequest;
import com.example.eventmanagement.dto.response.AttendeeResponse;
import com.example.eventmanagement.entity.Attendee;
import org.springframework.stereotype.Component;

@Component
public class AttendeeMapper {

    public Attendee toEntity(AttendeeRequest request) {
        if (request == null) {
            return null;
        }
        Attendee attendee = new Attendee();
        attendee.setName(request.getName());
        attendee.setEmail(request.getEmail());
        return attendee;
    }

    public AttendeeResponse toResponse(Attendee attendee) {
        if (attendee == null) {
            return null;
        }
        AttendeeResponse response = new AttendeeResponse();
        response.setId(attendee.getId());
        response.setName(attendee.getName());
        response.setEmail(attendee.getEmail());
        return response;
    }

    public void updateEntity(AttendeeRequest request, Attendee attendee) {
        if (request == null || attendee == null) {
            return;
        }
        attendee.setName(request.getName());
        attendee.setEmail(request.getEmail());
    }
}
