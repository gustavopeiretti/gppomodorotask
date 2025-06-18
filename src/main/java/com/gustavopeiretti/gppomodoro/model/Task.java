package com.gustavopeiretti.gppomodoro.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private LocalDateTime creationDate;

    private LocalDateTime completionDate;

    @ManyToOne(fetch = FetchType.EAGER) // EAGER para cargarla siempre con la tarea
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private boolean completed = false;

    private int pomodorosSpent = 0;


    public Task() {
        this.creationDate = LocalDateTime.now();
    }

    public Task(String name, Category category) {
        this();
        this.name = name;
        this.category = category;
    }

    public Task(String name, String description, Category category) {
        this(name, category);
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDateTime completionDate) {
        this.completionDate = completionDate;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && this.completionDate == null) {
            this.completionDate = LocalDateTime.now();
        } else if (!completed) {
            this.completionDate = null;
        }
    }

    public int getPomodorosSpent() {
        return pomodorosSpent;
    }

    public void setPomodorosSpent(int pomodorosSpent) {
        this.pomodorosSpent = pomodorosSpent;
    }

    public void incrementPomodorosSpent() {
        this.pomodorosSpent++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ID: %d | Tarea: %s | Categoría: %s | Pomodoros: %d | %s",
                id, name, category.getName(), pomodorosSpent, completed ? "Completada" : "Pendiente");
    }
}