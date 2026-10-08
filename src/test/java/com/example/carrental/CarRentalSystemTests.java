package com.example.carrental;

import com.example.carrental.exception.BookingException;
import com.example.carrental.model.*;
import com.example.carrental.repository.*;
import com.example.carrental.service.AvailabilityService;
import com.example.carrental.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CarRentalSystemTests {

    @Autowired private BookingService bookingService;
    @Autowired private AvailabilityService availabilityService;
    @Autowired private UserRepository userRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private BookingStatusHistoryRepository historyRepository;

    private User user1;
    private User user2;
    private User admin;
    private Resource activeVehicle;
    private Resource unavailableVehicle;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        historyRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        user1  = userRepository.save(new User("User One", "user1", "user123", "USER", "ACTIVE"));
        user2  = userRepository.save(new User("User Two", "user2", "user123", "USER", "ACTIVE"));
        admin  = userRepository.save(new User("Administrator", "admin", "admin123", "ADMIN", "ACTIVE"));

        activeVehicle      = resourceRepository.save(new Resource("R101", "Vehicle A", "Sedan", 499.0, 10, "ACTIVE"));
        unavailableVehicle = resourceRepository.save(new Resource("R104", "Vehicle D", "SUV", 1299.0, 0, "ACTIVE"));
    }

    // -------------------------------------------------------
    // T1 & T2 – Quantity Validation
    // -------------------------------------------------------
    @Test
    @DisplayName("T1 & T2: Quantity validation – only 1..5 accepted")
    void testQuantityValidation() {
        // Invalid
        assertThrows(BookingException.class, () -> availabilityService.validateQuantity(0));
        assertThrows(BookingException.class, () -> availabilityService.validateQuantity(6));
        assertThrows(BookingException.class, () -> availabilityService.validateQuantity(-1));
        assertThrows(BookingException.class, () -> availabilityService.validateQuantity(null));

        // Valid
        assertDoesNotThrow(() -> availabilityService.validateQuantity(1));
        assertDoesNotThrow(() -> availabilityService.validateQuantity(3));
        assertDoesNotThrow(() -> availabilityService.validateQuantity(5));
    }

    // -------------------------------------------------------
    // T3 – Unavailable Vehicle
    // -------------------------------------------------------
    @Test
    @DisplayName("T3: Booking unavailable vehicle is rejected – state unchanged")
    void testBookingUnavailableVehicle() {
        int stockBefore = unavailableVehicle.getAvailability();

        assertThrows(BookingException.class, () ->
                bookingService.confirmBooking(user1, unavailableVehicle.getId(), 1,
                        LocalDate.now().plusDays(1), "Test", "9876543210", "DL-111"));

        int stockAfter = resourceRepository.findById(unavailableVehicle.getId()).orElseThrow().getAvailability();
        assertEquals(stockBefore, stockAfter);
        assertEquals(0, reservationRepository.count());
    }

    // -------------------------------------------------------
    // T4 – Fare Calculation (BR4)
    // -------------------------------------------------------
    @Test
    @DisplayName("T4: Amount calculation – booking charge rules (BR4)")
    void testAmountCalculation() {
        // ₹999 → ₹50 charge
        assertEquals(50.0,  bookingService.calculateBookingCharge(999.0));
        assertEquals(1049.0, 999.0 + bookingService.calculateBookingCharge(999.0));

        // ₹1000 → ₹0 charge
        assertEquals(0.0,   bookingService.calculateBookingCharge(1000.0));
        assertEquals(1000.0, 1000.0 + bookingService.calculateBookingCharge(1000.0));

        // ₹1001 → ₹0 charge
        assertEquals(0.0,   bookingService.calculateBookingCharge(1001.0));
        assertEquals(1001.0, 1001.0 + bookingService.calculateBookingCharge(1001.0));
    }

    // -------------------------------------------------------
    // T5 – Complete Valid Reservation (BR7)
    // -------------------------------------------------------
    @Test
    @DisplayName("T5: Valid reservation – booking created, availability reduced, history logged")
    void testValidReservationFlow() {
        int stockBefore = activeVehicle.getAvailability();
        int qty = 2;

        Reservation r = bookingService.confirmBooking(user1, activeVehicle.getId(), qty,
                LocalDate.now().plusDays(2), "John Doe", "9876543210", "DL-998877");

        assertNotNull(r);
        assertNotNull(r.getId());
        assertEquals("CONFIRMED", r.getStatus());
        assertEquals(qty, r.getItems().get(0).getQuantity());
        assertEquals(499.0, r.getItems().get(0).getRate()); // rate snapshot

        int stockAfter = resourceRepository.findById(activeVehicle.getId()).orElseThrow().getAvailability();
        assertEquals(stockBefore - qty, stockAfter);

        List<BookingStatusHistory> history =
                historyRepository.findByReferenceIdOrderByEventAtAsc(r.getReference());
        assertFalse(history.isEmpty());
        assertEquals("NONE",      history.get(0).getOldStatus());
        assertEquals("CONFIRMED", history.get(0).getNewStatus());
    }

    // -------------------------------------------------------
    // T6 – Invalid Customer Details (BR5)
    // -------------------------------------------------------
    @Test
    @DisplayName("T6: Invalid customer details – rejected with no partial updates")
    void testInvalidCustomerDetails() {
        int stockBefore = activeVehicle.getAvailability();

        // Empty name
        assertThrows(BookingException.class, () ->
                bookingService.confirmBooking(user1, activeVehicle.getId(), 1, LocalDate.now(), "", "9876543210", "DL123"));

        // Invalid phone
        assertThrows(BookingException.class, () ->
                bookingService.confirmBooking(user1, activeVehicle.getId(), 1, LocalDate.now(), "John", "12345", "DL123"));

        // Past date
        assertThrows(BookingException.class, () ->
                bookingService.confirmBooking(user1, activeVehicle.getId(), 1,
                        LocalDate.now().minusDays(1), "John", "9876543210", "DL123"));

        int stockAfter = resourceRepository.findById(activeVehicle.getId()).orElseThrow().getAvailability();
        assertEquals(stockBefore, stockAfter);
        assertEquals(0, reservationRepository.count());
    }

    // -------------------------------------------------------
    // T7 – Insufficient Availability at Confirmation (BR6)
    // -------------------------------------------------------
    @Test
    @DisplayName("T7: Quantity exceeds availability – rejected")
    void testInsufficientAvailability() {
        // Asking 6 → violates BR1 first, rejected
        assertThrows(BookingException.class, () ->
                bookingService.confirmBooking(user1, activeVehicle.getId(), 6,
                        LocalDate.now(), "John", "9876543210", "DL123"));

        assertEquals(0, reservationRepository.count());
    }

    // -------------------------------------------------------
    // T8 – Access Restriction (BR10)
    // -------------------------------------------------------
    @Test
    @DisplayName("T8: User cannot cancel another user's booking")
    void testUserAccessRestrictions() {
        Reservation r = bookingService.confirmBooking(user1, activeVehicle.getId(), 1,
                LocalDate.now(), "User One", "9876543210", "DL-11111");

        // user2 tries to cancel user1's booking
        assertThrows(BookingException.class, () -> bookingService.cancelBooking(r.getId(), user2));
    }

    // -------------------------------------------------------
    // T9 – Admin Status Transitions (BR8)
    // -------------------------------------------------------
    @Test
    @DisplayName("T9: Admin status transitions are strictly sequential (BR8)")
    void testAdminStatusTransitions() {
        Reservation r = bookingService.confirmBooking(user1, activeVehicle.getId(), 1,
                LocalDate.now(), "User One", "9876543210", "DL-11111");

        // Invalid skip: CONFIRMED → COMPLETED
        assertThrows(BookingException.class, () ->
                bookingService.updateStatus(r.getId(), "COMPLETED", admin.getUsername()));

        // Valid: CONFIRMED → CHECKED_IN
        Reservation checkedIn = bookingService.updateStatus(r.getId(), "CHECKED_IN", admin.getUsername());
        assertEquals("CHECKED_IN", checkedIn.getStatus());

        // Valid: CHECKED_IN → COMPLETED
        Reservation completed = bookingService.updateStatus(r.getId(), "COMPLETED", admin.getUsername());
        assertEquals("COMPLETED", completed.getStatus());

        // No more transitions allowed
        assertThrows(BookingException.class, () ->
                bookingService.updateStatus(r.getId(), "CHECKED_IN", admin.getUsername()));
    }

    // -------------------------------------------------------
    // T10 – Double Cancellation (BR9)
    // -------------------------------------------------------
    @Test
    @DisplayName("T10: Cancel twice – first works, second rejected, availability restored once")
    void testCancellationLifecycle() {
        int stockBefore = activeVehicle.getAvailability();
        int qty = 2;

        Reservation r = bookingService.confirmBooking(user1, activeVehicle.getId(), qty,
                LocalDate.now(), "User One", "9876543210", "DL-11111");

        assertEquals(stockBefore - qty,
                resourceRepository.findById(activeVehicle.getId()).orElseThrow().getAvailability());

        // First cancellation
        Reservation cancelled = bookingService.cancelBooking(r.getId(), user1);
        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals(stockBefore,
                resourceRepository.findById(activeVehicle.getId()).orElseThrow().getAvailability());

        // Second cancellation → rejected (BR9)
        assertThrows(BookingException.class, () -> bookingService.cancelBooking(r.getId(), user1));
    }

    // -------------------------------------------------------
    // T12 – Rate Snapshot Integrity (BR3)
    // -------------------------------------------------------
    @Test
    @DisplayName("T12: Rate snapshot preserved after vehicle rate change")
    void testRateSnapshotIntegrity() {
        Reservation r = bookingService.confirmBooking(user1, activeVehicle.getId(), 1,
                LocalDate.now(), "User One", "9876543210", "DL-11111");

        assertEquals(499.0, r.getItems().get(0).getRate());

        // Change resource rate
        activeVehicle.setRate(999.0);
        resourceRepository.save(activeVehicle);

        // Old booking must retain original snapshot
        Reservation reloaded = reservationRepository.findById(r.getId()).orElseThrow();
        assertEquals(499.0, reloaded.getItems().get(0).getRate());
    }
}
