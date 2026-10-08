package com.example.carrental.repository;

import com.example.carrental.model.Reservation;
import com.example.carrental.model.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {
    List<ReservationItem> findByReservation(Reservation reservation);
}

