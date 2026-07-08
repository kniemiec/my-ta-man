package com.mytaman.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * A task: the smallest unit of work. Backed by a single {@code .md} file.
 *
 * <p>{@code id}, {@code name}, {@code state}, {@code due}, {@code created} live in the
 * file frontmatter; {@code description} is the markdown body. {@code projectId} is
 * <em>derived</em> from the containing folder (never persisted as a field) — the folder
 * location is the single source of truth for project membership.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {

    private String id;
    private String name;
    private String description;
    private String progress;
    private LocalDate due;
    private TaskState state;
    private LocalDate created;

    /** Derived from folder; {@code "inbox"} for project-less tasks. Not stored in frontmatter. */
    private String projectId;

    public Task() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public LocalDate getDue() {
        return due;
    }

    public void setDue(LocalDate due) {
        this.due = due;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
