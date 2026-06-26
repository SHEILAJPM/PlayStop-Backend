package com.playstop.backend.repository;

import com.playstop.backend.entity.Payment;
import com.playstop.backend.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByReservation(Reservation reservation);
    Optional<Payment> findByCulqiChargeId(String culqiChargeId);
}