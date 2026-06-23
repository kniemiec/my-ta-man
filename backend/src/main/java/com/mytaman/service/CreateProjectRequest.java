package com.mytaman.service;

import com.mytaman.model.ProjectState;

import java.time.LocalDate;

/** Body for {@code POST /api/projects}. */
public class CreateProjectRequest {
    public String name;
    public String description;
    public LocalDate due;
    public ProjectState state;
}
