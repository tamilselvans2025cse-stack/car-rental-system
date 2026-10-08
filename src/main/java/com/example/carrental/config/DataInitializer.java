package com.example.carrental.config;

import com.example.carrental.model.Resource;
import com.example.carrental.model.User;
import com.example.carrental.repository.ResourceRepository;
import com.example.carrental.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    public DataInitializer(UserRepository userRepository, ResourceRepository resourceRepository) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    public void run(String... args) {
        // Seed Sample Users if empty
        if (userRepository.count() == 0) {
            User user1 = new User("User One", "user1", "user123", "USER", "ACTIVE");
            User user2 = new User("User Two", "user2", "user123", "USER", "ACTIVE");
            User admin = new User("System Administrator", "admin", "admin123", "ADMIN", "ACTIVE");

            userRepository.saveAll(Arrays.asList(user1, user2, admin));
            System.out.println(">>> Sample Users Initialized: user1, user2, admin");
        }

        // Seed Sample Vehicles (Resources) if empty
        if (resourceRepository.count() == 0) {
            Resource r101 = new Resource("R101", "Vehicle A", "Sedan", 499.0, 10, "ACTIVE");
            Resource r102 = new Resource("R102", "Vehicle B", "SUV", 799.0, 5, "ACTIVE");
            Resource r103 = new Resource("R103", "Vehicle C", "Hatchback", 199.0, 20, "ACTIVE");
            Resource r104 = new Resource("R104", "Vehicle D", "SUV", 1299.0, 0, "ACTIVE");
            Resource r105 = new Resource("R105", "Vehicle E", "Sedan", 1000.0, 3, "ACTIVE");

            resourceRepository.saveAll(Arrays.asList(r101, r102, r103, r104, r105));
            System.out.println(">>> Sample Vehicles Initialized: R101 - R105");
        }
    }
}
