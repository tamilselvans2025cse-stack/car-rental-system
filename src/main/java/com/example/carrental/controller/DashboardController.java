package com.example.carrental.controller;

import com.example.carrental.model.Reservation;
import com.example.carrental.model.User;
import com.example.carrental.repository.ResourceRepository;
import com.example.carrental.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * User dashboard showing personal booking stats.
 */
@Controller
public class DashboardController {

    private final BookingService bookingService;
    private final ResourceRepository resourceRepository;

    public DashboardController(BookingService bookingService, ResourceRepository resourceRepository) {
        this.bookingService = bookingService;
        this.resourceRepository = resourceRepository;
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return "redirect:/admin";

        List<Reservation> userBookings = bookingService.getBookingsByUser(user);

        long activeBookings = userBookings.stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()) || "CHECKED_IN".equalsIgnoreCase(b.getStatus()))
                .count();

        long availableVehicles = resourceRepository.findAll().stream()
                .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()) && r.getAvailability() != null && r.getAvailability() > 0)
                .count();

        model.addAttribute("user", user);
        model.addAttribute("totalBookings", userBookings.size());
        model.addAttribute("activeBookings", activeBookings);
        model.addAttribute("availableVehicles", availableVehicles);
        model.addAttribute("recentBookings", userBookings.stream().limit(5).toList());
        return "dashboard";
    }
}
