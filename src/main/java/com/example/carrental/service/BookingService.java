package com.example.carrental.service;

import com.example.carrental.exception.BookingException;
import com.example.carrental.model.*;
import com.example.carrental.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class BookingService {

    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final ResourceRepository resourceRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ScheduleRepository scheduleRepository;
    private final AvailabilityService availabilityService;

    public BookingService(ReservationRepository reservationRepository,
                          ReservationItemRepository reservationItemRepository,
                          ResourceRepository resourceRepository,
                          BookingStatusHistoryRepository historyRepository,
                          ScheduleRepository scheduleRepository,
                          AvailabilityService availabilityService) {
        this.reservationRepository = reservationRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.resourceRepository = resourceRepository;
        this.historyRepository = historyRepository;
        this.scheduleRepository = scheduleRepository;
        this.availabilityService = availabilityService;
    }

    /**
     * BR4: Base Total = Rate × Quantity
     * Booking Charge:
     * If Base Total < ₹1000 => ₹50
     * If Base Total >= ₹1000 => ₹0
     * Grand Total = Base Total + Booking Charge
     */
    public double calculateBaseTotal(double rate, int quantity) {
        return rate * quantity;
    }

    public double calculateBaseAmount(double rate, int quantity) {
        return calculateBaseTotal(rate, quantity);
    }

    public double calculateBookingCharge(double baseTotal) {
        if (baseTotal < 1000.0) {
            return 50.0;
        } else {
            return 0.0;
        }
    }

    public double calculateGrandTotal(double rate, int quantity) {
        double base = calculateBaseTotal(rate, quantity);
        double charge = calculateBookingCharge(base);
        return base + charge;
    }

    /**
     * BR5: Validation of booking details
     */
    public void validateBookingDetails(String customerName, String customerPhone,
                                       LocalDate bookingDate, String identityProof) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new BookingException("Name cannot be empty.");
        }
        if (customerPhone == null || !customerPhone.trim().matches("^[0-9]{10}$")) {
            throw new BookingException("Phone number must contain exactly 10 digits.");
        }
        if (bookingDate == null || bookingDate.isBefore(LocalDate.now())) {
            throw new BookingException("Booking date must be a valid present or future date.");
        }
        if (identityProof == null || identityProof.trim().isEmpty()) {
            throw new BookingException("Booking details are invalid.");
        }
    }

    /**
     * FR4, FR5, BR1-BR7, Section 10:
     * Atomic transaction to confirm booking.
     */
    @Transactional
    public Reservation confirmBooking(User user, String resourceId, int quantity,
                                       LocalDate bookingDate, String customerName,
                                       String customerPhone, String identityProof) {

        // BR1: Validate quantity (1 to 5)
        availabilityService.validateQuantity(quantity);

        // BR5: Validate customer and booking details
        validateBookingDetails(customerName, customerPhone, bookingDate, identityProof);

        // Fetch resource
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BookingException("Vehicle not found."));

        // BR2 & BR6: Check availability again before confirmation
        if (!"ACTIVE".equalsIgnoreCase(resource.getStatus()) ||
                resource.getAvailability() == null || resource.getAvailability() < quantity) {
            throw new BookingException("Vehicle is currently unavailable.");
        }

        // BR3 & BR4: Calculate amounts using current vehicle rate (Rate Snapshot)
        double snapshotRate = resource.getRate();
        double baseAmount = calculateBaseTotal(snapshotRate, quantity);
        double bookingCharge = calculateBookingCharge(baseAmount);
        double grandTotal = baseAmount + bookingCharge;

        // Generate unique booking reference e.g., BK-100234
        String reference = generateUniqueReference();

        // BR7: Step 1 - Create CONFIRMED reservation
        Reservation reservation = new Reservation();
        reservation.setReference(reference);
        reservation.setDate(bookingDate);
        reservation.setStatus("CONFIRMED");
        reservation.setBaseAmount(baseAmount);
        reservation.setBookingCharge(bookingCharge);
        reservation.setTotalAmount(grandTotal);
        reservation.setCustomerName(customerName.trim());
        reservation.setCustomerPhone(customerPhone.trim());
        reservation.setIdentityProof(identityProof.trim());
        reservation.setUser(user);
        reservation.setCreatedAt(LocalDateTime.now());

        // BR7: Step 2 - Save Reservation Item with Rate Snapshot
        ReservationItem item = new ReservationItem();
        item.setQuantity(quantity);
        item.setRate(snapshotRate); // Immutable rate snapshot
        item.setResource(resource);
        item.setReservation(reservation);
        reservation.addItem(item);

        Reservation savedReservation = reservationRepository.save(reservation);

        // BR7: Step 3 - Reduce availability
        availabilityService.reduceAvailability(resourceId, quantity);

        // BR7: Step 4 - Record initial status history
        BookingStatusHistory history = new BookingStatusHistory(
                reference,
                user.getUsername(),
                "NONE",
                "CONFIRMED",
                LocalDateTime.now()
        );
        historyRepository.save(history);

        // Record Schedule entry
        Schedule schedule = new Schedule(resourceId, bookingDate, bookingDate.plusDays(1));
        scheduleRepository.save(schedule);

        return savedReservation;
    }

    /**
     * BR9 & BR10: Cancellation
     * Only CONFIRMED bookings can be cancelled.
     * Restores availability exactly once.
     */
    @Transactional
    public Reservation cancelBooking(Long bookingId, User user) {
        Reservation reservation = reservationRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found."));

        // BR10: Access control
        if (!reservation.getUser().getId().equals(user.getId()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BookingException("You are not authorized to access this booking.");
        }

        // BR9: Only CONFIRMED bookings can be cancelled
        if (!"CONFIRMED".equalsIgnoreCase(reservation.getStatus())) {
            throw new BookingException("Booking cannot be cancelled.");
        }

        // Restore availability exactly once
        for (ReservationItem item : reservation.getItems()) {
            availabilityService.restoreAvailability(item.getResource().getId(), item.getQuantity());
        }

        // Update status to CANCELLED
        String oldStatus = reservation.getStatus();
        reservation.setStatus("CANCELLED");
        Reservation updated = reservationRepository.save(reservation);

        // Record history
        BookingStatusHistory history = new BookingStatusHistory(
                reservation.getReference(),
                user.getUsername(),
                oldStatus,
                "CANCELLED",
                LocalDateTime.now()
        );
        historyRepository.save(history);

        return updated;
    }

    /**
     * BR8: Admin status transition
     * Allowed sequence: CONFIRMED -> CHECKED_IN -> COMPLETED
     * Cannot skip statuses.
     */
    @Transactional
    public Reservation updateStatus(Long bookingId, String targetStatus, String adminUsername) {
        Reservation reservation = reservationRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found."));

        String currentStatus = reservation.getStatus();

        boolean isValid = false;
        if ("CONFIRMED".equalsIgnoreCase(currentStatus) && "CHECKED_IN".equalsIgnoreCase(targetStatus)) {
            isValid = true;
        } else if ("CHECKED_IN".equalsIgnoreCase(currentStatus) && "COMPLETED".equalsIgnoreCase(targetStatus)) {
            isValid = true;
        }

        if (!isValid) {
            throw new BookingException("Invalid status transition.");
        }

        reservation.setStatus(targetStatus);
        Reservation updated = reservationRepository.save(reservation);

        // Record status history
        BookingStatusHistory history = new BookingStatusHistory(
                reservation.getReference(),
                adminUsername,
                currentStatus,
                targetStatus,
                LocalDateTime.now()
        );
        historyRepository.save(history);

        return updated;
    }

    @Transactional
    public Reservation updateStatus(Long bookingId, String targetStatus, User adminUser) {
        if (adminUser == null || !"ADMIN".equalsIgnoreCase(adminUser.getRole())) {
            throw new BookingException("You are not authorized to access this booking.");
        }
        return updateStatus(bookingId, targetStatus, adminUser.getUsername());
    }

    public Reservation getBookingById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new BookingException("Booking not found."));
    }

    public Reservation findById(Long id) {
        return getBookingById(id);
    }

    public Reservation getBookingByReference(String reference) {
        return reservationRepository.findByReference(reference)
                .orElseThrow(() -> new BookingException("Booking with reference " + reference + " not found."));
    }

    public Reservation findByReference(String reference) {
        return getBookingByReference(reference);
    }

    public List<Reservation> getBookingsByUser(User user) {
        return reservationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Reservation> findUserBookings(User user) {
        return getBookingsByUser(user);
    }

    public List<Reservation> getAllBookings() {
        return reservationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Reservation> findAllBookings() {
        return getAllBookings();
    }

    public List<BookingStatusHistory> getBookingHistory(String reference) {
        return historyRepository.findByReferenceIdOrderByEventAtAsc(reference);
    }

    public long countBookingsByStatus(String status) {
        return reservationRepository.countByStatus(status);
    }

    public long countByStatus(String status) {
        return countBookingsByStatus(status);
    }

    public long countTotalBookings() {
        return reservationRepository.count();
    }

    public ReservationRepository.BookingStatusCounts getBookingStatusCounts() {
        return reservationRepository.countBookingStatuses();
    }

    public long countAll() {
        return countTotalBookings();
    }

    private String generateUniqueReference() {
        Random random = new Random();
        int randomNum = 10000 + random.nextInt(90000);
        return "BK-" + randomNum;
    }
}
