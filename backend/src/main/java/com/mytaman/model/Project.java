package com.mytaman.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A project groups tasks. Backed by a folder at the vault root whose {@code _project.md}
 * file holds the metadata + description. The task list is derived from the folder contents
 * and is only populated when fetching a single project.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Project {

    private String id;
    private String name;
    private String description;
    private String progress;
    private LocalDate due;
    private ProjectState state;
    private LocalDate created;

    /** Number of tasks in the folder (set on list views). */
    private Integer taskCount;

    /** Full task list (set only when fetching a single project). */
    private List<Task> tasks;

    public Project() {
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

    public ProjectState getState() {
        return state;
    }

    public void setState(ProjectState state) {
        this.state = state;
    }

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public Integer getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(Integer taskCount) {
        this.taskCount = taskCount;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }
}
