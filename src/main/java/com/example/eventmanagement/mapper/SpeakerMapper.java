package com.example.eventmanagement.mapper;

import com.example.eventmanagement.dto.request.SpeakerRequest;
import com.example.eventmanagement.dto.response.SpeakerResponse;
import com.example.eventmanagement.entity.Speaker;
import org.springframework.stereotype.Component;

@Component
public class SpeakerMapper {

    public Speaker toEntity(SpeakerRequest request) {
        if (request == null) {
            return null;
        }
        Speaker speaker = new Speaker();
        speaker.setFirstName(request.getFirstName());
        speaker.setLastName(request.getLastName());
        speaker.setBio(request.getBio());
        return speaker;
    }

    public SpeakerResponse toResponse(Speaker speaker) {
        if (speaker == null) {
            return null;
        }
        SpeakerResponse response = new SpeakerResponse();
        response.setId(speaker.getId());
        response.setFirstName(speaker.getFirstName());
        response.setLastName(speaker.getLastName());
        response.setBio(speaker.getBio());
        return response;
    }

    public void updateEntity(SpeakerRequest request, Speaker speaker) {
        if (request == null || speaker == null) {
            return;
        }
        speaker.setFirstName(request.getFirstName());
        speaker.setLastName(request.getLastName());
        speaker.setBio(request.getBio());
    }
}
