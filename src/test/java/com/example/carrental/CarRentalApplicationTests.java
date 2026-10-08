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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
public class CarRentalApplicationTests {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationItemRepository reservationItemRepository;

    @Autowired
    private BookingStatusHistoryRepository historyRepository;

    private User testUser1;
    private User testUser2;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Clear transaction data for test isolation
        reservationItemRepository.deleteAll();
        historyRepository.deleteAll();
        reservationRepository.deleteAll();

        testUser1 = userRepository.findByUsername("user1")
                .orElseGet(() -> userRepository.save(new User("User One", "user1", "user123", "USER", "ACTIVE")));
        testUser2 = userRepository.findByUsername("user2")
                .orElseGet(() -> userRepository.save(new User("User Two", "user2", "user123", "USER", "ACTIVE")));
        adminUser = userRepository.findByUsername("admin")
                .orElseGet(() -> userRepository.save(new User("Admin", "admin", "admin123", "ADMIN", "ACTIVE")));
    }

    @Test
    @DisplayName("T1 & T5: Complete a valid reservation, check availability reduction and history")
    @Transactional
    void testValidReservationAndAvailabilityReduction() {
        Resource vehicle = resourceRepository.findById("R101").orElseThrow();
        int initialAvailability = vehicle.getAvailability();

        Reservation res = bookingService.confirmBooking(
                testUser1, "R101", 2, LocalDate.now().plusDays(2),
                "John Doe", "9876543210", "DL-TEST-1234"
        );

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("CONFIRMED", res.getStatus());
        assertEquals(2 * 499.0, res.getBaseAmount());
        assertEquals(50.0, res.getBookingCharge()); // 998 < 1000
        assertEquals(998.0 + 50.0, res.getTotalAmount());

        // Check availability reduced
        Resource updatedVehicle = resourceRepository.findById("R101").orElseThrow();
        assertEquals(initialAvailability - 2, updatedVehicle.getAvailability());

        // Check history recorded
        List<BookingStatusHistory> history = historyRepository.findByReferenceIdOrderByEventAtAsc(res.getReference());
        assertFalse(history.isEmpty());
        assertEquals("CONFIRMED", history.get(0).getNewStatus());
    }

    @Test
    @DisplayName("T2: Test quantity bounds (0, 1, 5, 6)")
    void testQuantityValidation() {
        // Invalid: 0
        BookingException ex0 = assertThrows(BookingException.class, () -> availabilityService.validateQuantity(0));
        assertEquals("Quantity must be between 1 and 5.", ex0.getMessage());

        // Invalid: 6
        BookingException ex6 = assertThrows(BookingException.class, () -> availabilityService.validateQuantity(6));
        assertEquals("Quantity must be between 1 and 5.", ex6.getMessage());

        // Invalid: negative
        BookingException exNeg = assertThrows(BookingException.class, () -> availabilityService.validateQuantity(-1));
        assertEquals("Quantity must be between 1 and 5.", exNeg.getMessage());

        // Valid: 1 and 5 should not throw
        assertDoesNotThrow(() -> availabilityService.validateQuantity(1));
        assertDoesNotThrow(() -> availabilityService.validateQuantity(5));
    }

    @Test
    @DisplayName("T3: Try booking an unavailable vehicle (R104 availability = 0)")
    void testUnavailableVehicleBookingFails() {
        Resource r104 = resourceRepository.findById("R104").orElseThrow();
        assertEquals(0, r104.getAvailability());

        BookingException ex = assertThrows(BookingException.class, () -> {
            bookingService.confirmBooking(
                    testUser1, "R104", 1, LocalDate.now().plusDays(1),
                    "John Doe", "9876543210", "DL-12345"
            );
        });

        assertEquals("Vehicle is currently unavailable.", ex.getMessage());
        assertEquals(0, reservationRepository.count());
    }

    @Test
    @DisplayName("T4: Test fare calculation & booking charge rules (₹999, ₹1000, ₹1001)")
    void testFareCalculationAndBookingCharge() {
        // Case 1: Base total < 1000 => Booking charge = 50
        assertEquals(50.0, bookingService.calculateBookingCharge(999.0));
        assertEquals(999.0 + 50.0, 999.0 + bookingService.calculateBookingCharge(999.0));

        // Case 2: Base total = 1000 => Booking charge = 0
        assertEquals(0.0, bookingService.calculateBookingCharge(1000.0));
        assertEquals(1000.0, 1000.0 + bookingService.calculateBookingCharge(1000.0));

        // Case 3: Base total > 1000 => Booking charge = 0
        assertEquals(0.0, bookingService.calculateBookingCharge(1001.0));
        assertEquals(1001.0, 1001.0 + bookingService.calculateBookingCharge(1001.0));
    }

    @Test
    @DisplayName("T6: Confirm with missing/invalid customer details")
    void testMissingOrInvalidCustomerDetails() {
        // Invalid Phone (less than 10 digits)
        BookingException exPhone = assertThrows(BookingException.class, () -> {
            bookingService.confirmBooking(
                    testUser1, "R101", 1, LocalDate.now().plusDays(1),
                    "John Doe", "12345", "DL-123"
            );
        });
        assertEquals("Phone number must contain exactly 10 digits.", exPhone.getMessage());

        // Empty Name
        BookingException exName = assertThrows(BookingException.class, () -> {
            bookingService.confirmBooking(
                    testUser1, "R101", 1, LocalDate.now().plusDays(1),
                    "  ", "9876543210", "DL-123"
            );
        });
        assertEquals("Name cannot be empty.", exName.getMessage());

        // Past Date
        BookingException exDate = assertThrows(BookingException.class, () -> {
            bookingService.confirmBooking(
                    testUser1, "R101", 1, LocalDate.now().minusDays(1),
                    "John Doe", "9876543210", "DL-123"
            );
        });
        assertEquals("Booking date must be a valid present or future date.", exDate.getMessage());
    }

    @Test
    @DisplayName("T7: Insufficient availability requested")
    void testInsufficientAvailability() {
        // R102 has 5 units available. Try requesting 5 when only 3 requested is okay, but requesting > availability fails
        Resource r105 = resourceRepository.findById("R105").orElseThrow();
        assertEquals(3, r105.getAvailability());

        // Requesting 4 units exceeds available 3
        BookingException ex = assertThrows(BookingException.class, () -> {
            bookingService.confirmBooking(
                    testUser1, "R105", 4, LocalDate.now().plusDays(1),
                    "John Doe", "9876543210", "DL-123"
            );
        });
        assertEquals("Vehicle is currently unavailable.", ex.getMessage());
    }

    @Test
    @DisplayName("T8: User cannot cancel another user's booking")
    @Transactional
    void testUnauthorizedUserAccess() {
        Reservation res = bookingService.confirmBooking(
                testUser1, "R103", 1, LocalDate.now().plusDays(1),
                "User One", "9876543210", "DL-111"
        );

        // testUser2 attempts to cancel testUser1's booking
        BookingException ex = assertThrows(BookingException.class, () -> {
            bookingService.cancelBooking(res.getId(), testUser2);
        });

        assertEquals("You are not authorized to access this booking.", ex.getMessage());
    }

    @Test
    @DisplayName("T9: Admin cannot skip status (CONFIRMED -> COMPLETED is invalid)")
    @Transactional
    void testAdminCannotSkipStatus() {
        Reservation res = bookingService.confirmBooking(
                testUser1, "R103", 1, LocalDate.now().plusDays(1),
                "User One", "9876543210", "DL-111"
        );

        // Attempt invalid skip transition: CONFIRMED -> COMPLETED
        BookingException ex = assertThrows(BookingException.class, () -> {
            bookingService.updateStatus(res.getId(), "COMPLETED", "admin");
        });
        assertEquals("Invalid status transition.", ex.getMessage());

        // Valid sequence: CONFIRMED -> CHECKED_IN -> COMPLETED
        assertDoesNotThrow(() -> bookingService.updateStatus(res.getId(), "CHECKED_IN", "admin"));
        Reservation updated1 = reservationRepository.findById(res.getId()).orElseThrow();
        assertEquals("CHECKED_IN", updated1.getStatus());

        assertDoesNotThrow(() -> bookingService.updateStatus(res.getId(), "COMPLETED", "admin"));
        Reservation updated2 = reservationRepository.findById(res.getId()).orElseThrow();
        assertEquals("COMPLETED", updated2.getStatus());
    }

    @Test
    @DisplayName("T10: Cancel confirmed booking restores availability once, second cancel fails")
    @Transactional
    void testCancellationAndAvailabilityRestoration() {
        Resource r103 = resourceRepository.findById("R103").orElseThrow();
        int initialAvail = r103.getAvailability();

        Reservation res = bookingService.confirmBooking(
                testUser1, "R103", 2, LocalDate.now().plusDays(1),
                "User One", "9876543210", "DL-111"
        );

        Resource afterBooking = resourceRepository.findById("R103").orElseThrow();
        assertEquals(initialAvail - 2, afterBooking.getAvailability());

        // Cancel first time: Success
        bookingService.cancelBooking(res.getId(), testUser1);

        Resource afterCancel = resourceRepository.findById("R103").orElseThrow();
        assertEquals(initialAvail, afterCancel.getAvailability()); // Restored!

        // Cancel second time: Rejection
        BookingException ex = assertThrows(BookingException.class, () -> {
            bookingService.cancelBooking(res.getId(), testUser1);
        });
        assertEquals("Booking cannot be cancelled.", ex.getMessage());
    }

    @Test
    @DisplayName("T12: Vehicle rate change does not alter existing booking rate snapshot")
    @Transactional
    void testRateSnapshotImmutable() {
        Resource r105 = resourceRepository.findById("R105").orElseThrow();
        assertEquals(1000.0, r105.getRate());

        Reservation res = bookingService.confirmBooking(
                testUser1, "R105", 1, LocalDate.now().plusDays(1),
                "User One", "9876543210", "DL-111"
        );

        // Change vehicle rate in catalog
        r105.setRate(1500.0);
        resourceRepository.save(r105);

        // Check that reservation item snapshot remained 1000.0
        Reservation reloaded = reservationRepository.findById(res.getId()).orElseThrow();
        assertEquals(1000.0, reloaded.getItems().get(0).getRate());
        assertEquals(1000.0, reloaded.getBaseAmount());
    }
}
