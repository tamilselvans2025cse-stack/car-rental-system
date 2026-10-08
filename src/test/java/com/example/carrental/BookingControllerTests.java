package com.example.carrental;

import com.example.carrental.model.Resource;
import com.example.carrental.model.User;
import com.example.carrental.repository.BookingStatusHistoryRepository;
import com.example.carrental.repository.ReservationRepository;
import com.example.carrental.repository.ResourceRepository;
import com.example.carrental.repository.ScheduleRepository;
import com.example.carrental.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private BookingStatusHistoryRepository historyRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private UserRepository userRepository;

    private User user;
    private Resource vehicle;

    @BeforeEach
    void setUp() {
        scheduleRepository.deleteAll();
        reservationRepository.deleteAll();
        historyRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("Test User", "booking-test-user", "password", "USER", "ACTIVE"));
        vehicle = resourceRepository.save(new Resource("BOOK-101", "Test Vehicle", "Sedan", 499.0, 3, "ACTIVE"));
    }

    @Test
    void formSubmissionCreatesBookingAndRendersConfirmation() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        mockMvc.perform(post("/booking/confirm")
                        .session(session)
                        .param("resourceId", vehicle.getId())
                        .param("quantity", "1")
                        .param("bookingDate", LocalDate.now().plusDays(1).toString())
                        .param("customerName", "Test User")
                        .param("customerPhone", "9876543210")
                        .param("identityProof", "DL-12345"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking"))
                .andExpect(model().attributeExists("reservation"))
                .andExpect(content().string(containsString("Booking Confirmed!")));
    }

    @Test
    void invalidBookingRendersErrorOnBookingForm() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        mockMvc.perform(post("/booking/confirm")
                        .session(session)
                        .param("resourceId", vehicle.getId())
                        .param("quantity", "1")
                        .param("bookingDate", LocalDate.now().plusDays(1).toString())
                        .param("customerName", "Test User")
                        .param("customerPhone", "9876543210")
                        .param("identityProof", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("booking"))
                .andExpect(model().attribute("errorMessage", "Booking details are invalid."))
                .andExpect(content().string(containsString("Booking details are invalid.")));
    }
}
