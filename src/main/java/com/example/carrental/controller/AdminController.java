package com.example.carrental.controller;

import com.example.carrental.exception.BookingException;
import com.example.carrental.model.Reservation;
import com.example.carrental.model.User;
import com.example.carrental.repository.ReservationRepository;
import com.example.carrental.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin dashboard and booking status management.
 */
@Controller
public class AdminController {

    private final BookingService bookingService;

    public AdminController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/admin")
    public String showAdminDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BookingException("You are not authorized to access this booking.");
        }

        List<Reservation> allBookings = bookingService.getAllBookings();
        ReservationRepository.BookingStatusCounts counts = bookingService.getBookingStatusCounts();

        model.addAttribute("user", user);
        model.addAttribute("bookings", allBookings);
        model.addAttribute("totalCount", counts.getTotalCount());
        model.addAttribute("confirmedCount", counts.getConfirmedCount());
        model.addAttribute("checkedInCount", counts.getCheckedInCount());
        model.addAttribute("completedCount", counts.getCompletedCount());
        model.addAttribute("cancelledCount", counts.getCancelledCount());

        return "admin";
    }

    @PostMapping("/admin/update-status")
    public String updateBookingStatus(@RequestParam("bookingId") Long bookingId,
                                      @RequestParam("targetStatus") String nextStatus,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BookingException("You are not authorized to access this booking.");
        }

        try {
            bookingService.updateStatus(bookingId, nextStatus, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking status updated to " + nextStatus + " successfully.");
        } catch (BookingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin";
    }
}
