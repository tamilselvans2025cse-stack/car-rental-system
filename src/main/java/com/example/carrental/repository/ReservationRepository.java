package com.example.carrental.repository;

import com.example.carrental.model.Reservation;
import com.example.carrental.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserOrderByCreatedAtDesc(User user);
    Optional<Reservation> findByReference(String reference);

    @EntityGraph(attributePaths = {"user", "items", "items.resource"})
    List<Reservation> findAllByOrderByCreatedAtDesc();

    @Query("""
            select count(r) as totalCount,
                   sum(case when r.status = 'CONFIRMED' then 1 else 0 end) as confirmedCount,
                   sum(case when r.status = 'CHECKED_IN' then 1 else 0 end) as checkedInCount,
                   sum(case when r.status = 'COMPLETED' then 1 else 0 end) as completedCount,
                   sum(case when r.status = 'CANCELLED' then 1 else 0 end) as cancelledCount
            from Reservation r
            """)
    BookingStatusCounts countBookingStatuses();

    long countByStatus(String status);

    interface BookingStatusCounts {
        long getTotalCount();
        long getConfirmedCount();
        long getCheckedInCount();
        long getCompletedCount();
        long getCancelledCount();
    }
}
