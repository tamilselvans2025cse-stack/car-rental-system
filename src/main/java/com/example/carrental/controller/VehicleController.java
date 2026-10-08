package com.example.carrental.controller;

import com.example.carrental.model.Resource;
import com.example.carrental.model.User;
import com.example.carrental.repository.ResourceRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Vehicle browsing and search (FR1).
 */
@Controller
public class VehicleController {

    private final ResourceRepository resourceRepository;

    public VehicleController(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @GetMapping("/vehicles")
    public String listVehicles(@RequestParam(value = "search", required = false) String search,
                               HttpSession session,
                               Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        List<Resource> vehicles;
        if (search != null && !search.trim().isEmpty()) {
            vehicles = resourceRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                    search.trim(), search.trim());
        } else {
            vehicles = resourceRepository.findAll();
        }

        model.addAttribute("user", user);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("searchQuery", search != null ? search.trim() : "");
        return "vehicles";
    }
}
