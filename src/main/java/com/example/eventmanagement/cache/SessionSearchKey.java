package com.example.eventmanagement.cache;

import lombok.Value;

@Value
public class SessionSearchKey {
    String speakerFirstName;
    String title;
    int page;
    int size;
    String sort;
}
