package com.example.eventmanagement.exception;

public class SpeakerNotFoundException extends RuntimeException {
    public SpeakerNotFoundException(String message) {
        super(message);
    }
}
