package com.example.eventmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EventManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventManagementApplication.class, args);
    }
}
