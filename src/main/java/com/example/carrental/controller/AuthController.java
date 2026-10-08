package com.example.carrental.controller;

import com.example.carrental.model.User;
import com.example.carrental.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Handles login and logout.
 */
@Controller
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return "ADMIN".equalsIgnoreCase(user.getRole()) ? "redirect:/admin" : "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return "ADMIN".equalsIgnoreCase(user.getRole()) ? "redirect:/admin" : "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpSession session,
                               Model model) {
        Optional<User> optionalUser = userRepository.findByUsername(username.trim());

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getPassword().equals(password.trim()) && "ACTIVE".equalsIgnoreCase(user.getStatus())) {
                session.setAttribute("user", user);
                return "ADMIN".equalsIgnoreCase(user.getRole()) ? "redirect:/admin" : "redirect:/dashboard";
            }
        }

        model.addAttribute("error", "Invalid username or password.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
