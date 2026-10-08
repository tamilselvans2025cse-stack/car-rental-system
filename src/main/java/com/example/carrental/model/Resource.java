package com.example.carrental.model;

import jakarta.persistence.*;

/**
 * Entity representing a Vehicle / Resource.
 * Example: R101 | Vehicle A | Sedan | 499 | 10 | ACTIVE
 */
@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @Column(nullable = false, length = 32)
    private String id; // e.g. R101, R102

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double rate;

    @Column(nullable = false)
    private Integer availability;

    @Column(nullable = false)
    private String status; // "ACTIVE"

    public Resource() {
    }

    public Resource(String id, String name, String category, Double rate, Integer availability, String status) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rate = rate;
        this.availability = availability;
        this.status = status;
    }

    // Getters and Setters

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Integer getAvailability() {
        return availability;
    }

    public void setAvailability(Integer availability) {
        this.availability = availability;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

