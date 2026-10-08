package com.example.carrental;

import com.example.carrental.model.Reservation;
import com.example.carrental.model.Resource;
import com.example.carrental.model.User;
import com.example.carrental.repository.BookingStatusHistoryRepository;
import com.example.carrental.repository.ReservationRepository;
import com.example.carrental.repository.ResourceRepository;
import com.example.carrental.repository.ScheduleRepository;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookingService bookingService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private BookingStatusHistoryRepository historyRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private UserRepository userRepository;

    private User admin;
    private User customer;
    private Resource vehicle;

    @BeforeEach
    void setUp() {
        scheduleRepository.deleteAll();
        reservationRepository.deleteAll();
        historyRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        admin = userRepository.save(new User("Administrator", "admin-test", "password", "ADMIN", "ACTIVE"));
        customer = userRepository.save(new User("Test Customer", "customer-test", "password", "USER", "ACTIVE"));
        vehicle = resourceRepository.save(new Resource("ADMIN-101", "Test Vehicle", "Sedan", 499.0, 3, "ACTIVE"));
    }

    @Test
    void adminDashboardLoadsForAdmin() throws Exception {
        bookingService.confirmBooking(customer, vehicle.getId(), 1, LocalDate.now().plusDays(1),
                "Test Customer", "9876543210", "DL-12345");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", admin);

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("totalCount", 1L))
                .andExpect(model().attribute("confirmedCount", 1L))
                .andExpect(model().attribute("checkedInCount", 0L))
                .andExpect(model().attribute("completedCount", 0L))
                .andExpect(model().attribute("cancelledCount", 0L))
                .andExpect(content().string(containsString("Operations Console")))
                .andExpect(content().string(containsString("Total Bookings")))
                .andExpect(content().string(containsString("Test Vehicle")));
    }

    @Test
    void adminCanUpdateStatusUsingFormParameter() throws Exception {
        Reservation reservation = bookingService.confirmBooking(
                customer, vehicle.getId(), 1, LocalDate.now().plusDays(1),
                "Test Customer", "9876543210", "DL-12345");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", admin);

        mockMvc.perform(post("/admin/update-status")
                        .session(session)
                        .param("bookingId", reservation.getId().toString())
                        .param("targetStatus", "CHECKED_IN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        assertEquals("CHECKED_IN",
                reservationRepository.findById(reservation.getId()).orElseThrow().getStatus());
    }
}
