package com.example.carrental.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a vehicle reservation.
 * Relationship: Many Reservations belong to one User.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 32)
    private String status; // "CONFIRMED", "CHECKED_IN", "COMPLETED", "CANCELLED"

    @Column(nullable = false)
    private Double baseAmount;

    @Column(nullable = false)
    private Double bookingCharge;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String customerPhone;

    @Column(nullable = false)
    private String identityReference;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReservationItem> items = new ArrayList<>();

    public Reservation() {
    }

    public Reservation(String reference, LocalDate date, String status, Double baseAmount,
                       Double bookingCharge, Double totalAmount, String customerName,
                       String customerPhone, String identityReference, User user) {
        this.reference = reference;
        this.date = date;
        this.status = status;
        this.baseAmount = baseAmount;
        this.bookingCharge = bookingCharge;
        this.totalAmount = totalAmount;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.identityReference = identityReference;
        this.user = user;
    }

    public void addItem(ReservationItem item) {
        items.add(item);
        item.setReservation(this);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(Double baseAmount) {
        this.baseAmount = baseAmount;
    }

    public Double getBookingCharge() {
        return bookingCharge;
    }

    public void setBookingCharge(Double bookingCharge) {
        this.bookingCharge = bookingCharge;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getIdentityReference() {
        return identityReference;
    }

    public void setIdentityReference(String identityReference) {
        this.identityReference = identityReference;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Column(nullable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getIdentityProof() {
        return identityReference;
    }

    public void setIdentityProof(String identityProof) {
        this.identityReference = identityProof;
    }

    public List<ReservationItem> getItems() {
        return items;
    }

    public void setItems(List<ReservationItem> items) {
        this.items = items;
    }
}
