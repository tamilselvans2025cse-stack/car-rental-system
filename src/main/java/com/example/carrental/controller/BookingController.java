package com.example.carrental.controller;

import com.example.carrental.exception.BookingException;
import com.example.carrental.model.Reservation;
import com.example.carrental.model.Resource;
import com.example.carrental.model.User;
import com.example.carrental.repository.ResourceRepository;
import com.example.carrental.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Booking creation, confirmation, listing, and cancellation.
 */
@Controller
public class BookingController {

    private final BookingService bookingService;
    private final ResourceRepository resourceRepository;

    public BookingController(BookingService bookingService, ResourceRepository resourceRepository) {
        this.bookingService = bookingService;
        this.resourceRepository = resourceRepository;
    }

    /** Show booking form (FR2) */
    @GetMapping("/booking")
    public String showBookingForm(@RequestParam("resourceId") String resourceId,
                                  HttpSession session,
                                  Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Resource vehicle = resourceRepository.findById(resourceId).orElse(null);
        if (vehicle == null || !"ACTIVE".equalsIgnoreCase(vehicle.getStatus()) || vehicle.getAvailability() <= 0) {
            model.addAttribute("errorMessage", "Vehicle is currently unavailable for booking.");
            return "error";
        }

        model.addAttribute("user", user);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("minDate", LocalDate.now().toString());
        model.addAttribute("defaultDate", LocalDate.now().toString());
        return "booking";
    }

    /** Process booking confirmation (FR4, FR5, BR1-BR7) */
    @PostMapping("/booking/confirm")
    public String confirmBooking(@RequestParam("resourceId") String resourceId,
                                 @RequestParam(value = "quantity", required = false) Integer quantity,
                                 @RequestParam(value = "bookingDate", required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
                                 @RequestParam(value = "customerName", required = false) String customerName,
                                 @RequestParam(value = "customerPhone", required = false) String customerPhone,
                                 @RequestParam(value = "identityProof", required = false) String identityProof,
                                 HttpSession session,
                                 Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Resource vehicle = resourceRepository.findById(resourceId).orElse(null);

        try {
            int qty = (quantity == null) ? 0 : quantity;
            Reservation reservation = bookingService.confirmBooking(
                    user, resourceId, qty, bookingDate, customerName, customerPhone, identityProof
            );
            model.addAttribute("user", user);
            model.addAttribute("reservation", reservation);
            return "booking";
        } catch (BookingException ex) {
            model.addAttribute("user", user);
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("minDate", LocalDate.now().toString());
            model.addAttribute("defaultDate", LocalDate.now().toString());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("enteredQuantity", quantity);
            model.addAttribute("enteredCustomerName", customerName);
            model.addAttribute("enteredCustomerPhone", customerPhone);
            model.addAttribute("enteredIdentityProof", identityProof);
            model.addAttribute("enteredBookingDate", bookingDate != null ? bookingDate.toString() : "");
            return "booking";
        }
    }

    /** My Bookings list (FR6, BR10) */
    @GetMapping("/bookings")
    public String showMyBookings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        List<Reservation> userBookings = bookingService.getBookingsByUser(user);
        model.addAttribute("bookings", userBookings);
        model.addAttribute("user", user);
        return "bookings";
    }

    /** Cancel booking (FR8, BR9, BR10) */
    @PostMapping("/booking/cancel")
    public String cancelBooking(@RequestParam("bookingId") Long bookingId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        try {
            bookingService.cancelBooking(bookingId, user);
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully. Vehicle availability restored.");
        } catch (BookingException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/bookings";
    }
}
