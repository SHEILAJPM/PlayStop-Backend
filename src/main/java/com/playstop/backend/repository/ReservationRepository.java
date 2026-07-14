package com.playstop.backend.repository;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByUser(User user);

    List<Reservation> findByCourt(Court court);

    @Query("SELECT r.slotHour, r.durationHours FROM Reservation r WHERE r.court = :court AND r.date = :date AND r.status != :cancelled")
    List<Object[]> findOccupiedRanges(Court court, LocalDate date, ReservationStatus cancelled);

    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r
        WHERE r.court = :court AND r.date = :date AND r.status <> :cancelled
          AND r.slotHour < :endHour
          AND (r.slotHour + r.durationHours) > :startHour
        """)
    boolean existsOverlapping(
        @Param("court") Court court, @Param("date") LocalDate date,
        @Param("startHour") int startHour, @Param("endHour") int endHour,
        @Param("cancelled") ReservationStatus cancelled
    );

    // ✅ Nuevo — usado por ReminderScheduler
    List<Reservation> findByDateAndSlotHourAndStatusAndReminderSentFalse(
        LocalDate date, int slotHour, ReservationStatus status
    );

    long countByStatus(ReservationStatus status);

    List<Reservation> findByUser_Role(com.playstop.backend.enums.Role role);

    long countByUser(User user);

    long countByUserAndStatus(User user, ReservationStatus status);

    @Query("SELECT COUNT(DISTINCT r.court.sportType) FROM Reservation r WHERE r.user = :user AND r.status = 'ATTENDED'")
    long countDistinctAttendedSportsByUser(@Param("user") User user);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.court c JOIN FETCH c.owner WHERE r.id = :id")
    java.util.Optional<Reservation> findByIdWithDetails(@Param("id") UUID id);

    // ─── BAN DE CUENTA ───────────────────────────────────────────────────────

    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND r.date = :date AND r.status IN :statuses")
    List<Reservation> findActiveReservationsByUserAndDate(
        @Param("userId") UUID userId,
        @Param("date") LocalDate date,
        @Param("statuses") List<ReservationStatus> statuses
    );

    // ─── ANALYTICS DE PROPIETARIO ─────────────────────────────────────────────

    @Query("""
        SELECT r.date, SUM(r.totalAmount)
        FROM Reservation r
        WHERE r.court.owner = :owner
          AND r.status IN ('CONFIRMED', 'ATTENDED')
          AND r.date >= :since
        GROUP BY r.date
        ORDER BY r.date
        """)
    List<Object[]> findDailyRevenueByOwner(
        @Param("owner") com.playstop.backend.entity.User owner,
        @Param("since") LocalDate since
    );

    @Query("""
        SELECT r.slotHour, COUNT(r)
        FROM Reservation r
        WHERE r.court.owner = :owner
          AND r.status <> 'CANCELLED'
        GROUP BY r.slotHour
        ORDER BY r.slotHour
        """)
    List<Object[]> findPeakHoursByOwner(@Param("owner") com.playstop.backend.entity.User owner);

    @Query("""
        SELECT r.court.name, COUNT(r), SUM(r.totalAmount)
        FROM Reservation r
        WHERE r.court.owner = :owner
          AND r.status IN ('CONFIRMED', 'ATTENDED')
        GROUP BY r.court.id, r.court.name
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> findCourtStatsByOwner(@Param("owner") com.playstop.backend.entity.User owner);

    @Query("""
        SELECT COUNT(r), COALESCE(SUM(r.totalAmount), 0)
        FROM Reservation r
        WHERE r.court.owner = :owner
          AND r.status IN ('CONFIRMED', 'ATTENDED')
        """)
    Object[] findTotalStatsByOwner(@Param("owner") com.playstop.backend.entity.User owner);

    @Query("""
        SELECT COUNT(r)
        FROM Reservation r
        WHERE r.court.owner = :owner
          AND r.status IN ('CONFIRMED', 'ATTENDED')
          AND r.date >= :since
        """)
    long countReservationsByOwnerSince(
        @Param("owner") com.playstop.backend.entity.User owner,
        @Param("since") LocalDate since
    );
}