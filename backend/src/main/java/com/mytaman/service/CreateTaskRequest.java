package com.mytaman.service;

import com.mytaman.model.TaskState;

import java.time.LocalDate;

/** Body for {@code POST /api/tasks}. {@code projectId} defaults to the Inbox. */
public class CreateTaskRequest {
    public String name;
    public String description;
    public String progress;
    public LocalDate due;
    public TaskState state;
    public String projectId;
}
