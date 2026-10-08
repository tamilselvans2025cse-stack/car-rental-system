package com.example.carrental.service;

import com.example.carrental.exception.BookingException;
import com.example.carrental.model.Resource;
import com.example.carrental.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private final ResourceRepository resourceRepository;

    public AvailabilityService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * BR1: User can reserve only 1 to 5 units in one booking.
     * Valid: 1, 2, 3, 4, 5.
     * Invalid: 0, 6, negative values, null, etc.
     */
    public void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 5) {
            throw new BookingException("Quantity must be between 1 and 5.");
        }
    }

    public void validateQuantity(int quantity) {
        validateQuantity((Integer) quantity);
    }

    /**
     * BR2: A vehicle can be reserved only if status = ACTIVE and availability >= requested quantity.
     */
    public boolean checkAvailability(String resourceId, int quantity) {
        validateQuantity(quantity);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BookingException("Vehicle not found."));

        if (!"ACTIVE".equalsIgnoreCase(resource.getStatus())) {
            return false;
        }

        return resource.getAvailability() != null && resource.getAvailability() >= quantity;
    }

    /**
     * Reduces vehicle availability by requested quantity during booking confirmation.
     */
    @Transactional
    public void reduceAvailability(String resourceId, int quantity) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BookingException("Vehicle not found."));

        if (!checkAvailability(resourceId, quantity)) {
            throw new BookingException("Vehicle is currently unavailable.");
        }

        resource.setAvailability(resource.getAvailability() - quantity);
        resourceRepository.save(resource);
    }

    /**
     * BR9: Restores vehicle availability when an eligible booking is cancelled.
     */
    @Transactional
    public void restoreAvailability(String resourceId, int quantity) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BookingException("Vehicle not found."));

        resource.setAvailability(resource.getAvailability() + quantity);
        resourceRepository.save(resource);
    }
}

