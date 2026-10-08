package com.example.carrental.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity tracking history of every booking status change.
 */
@Entity
@Table(name = "booking_status_history")
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String referenceId;

    @Column(nullable = false, length = 64)
    private String actorId;

    @Column(nullable = false, length = 32)
    private String oldStatus;

    @Column(nullable = false, length = 32)
    private String newStatus;

    @Column(nullable = false)
    private LocalDateTime eventAt;

    public BookingStatusHistory() {
    }

    public BookingStatusHistory(String referenceId, String actorId, String oldStatus, String newStatus, LocalDateTime eventAt) {
        this.referenceId = referenceId;
        this.actorId = actorId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.eventAt = eventAt;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getEventAt() {
        return eventAt;
    }

    public void setEventAt(LocalDateTime eventAt) {
        this.eventAt = eventAt;
    }
}

