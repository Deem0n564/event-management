package com.example.eventmanagement.controller;

import com.example.eventmanagement.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
public class AsyncTaskController {

    private final AsyncTaskService asyncTaskService;

    @PostMapping("/notify/{eventId}")
    public String startNotification(@PathVariable Long eventId) {
        String taskId = asyncTaskService.startAsyncTask(eventId);
        return "Task ID: " + taskId;
    }

    @GetMapping("/task/{taskId}")
    public String getTaskStatus(@PathVariable Long taskId) {
        String status = asyncTaskService.getTaskStatus(String.valueOf(taskId));
        return "Task status: " + status;
    }
}
