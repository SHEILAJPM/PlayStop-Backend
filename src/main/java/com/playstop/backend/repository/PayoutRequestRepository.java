package com.playstop.backend.repository;

import com.playstop.backend.entity.PayoutRequest;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, java.util.UUID> {

    List<PayoutRequest> findByOwnerOrderByRequestedAtDesc(User owner);

    @Query("SELECT p FROM PayoutRequest p JOIN FETCH p.owner ORDER BY p.requestedAt DESC")
    List<PayoutRequest> findAllByOrderByRequestedAtDesc();

    @Query("SELECT p FROM PayoutRequest p JOIN FETCH p.owner WHERE p.status = :status ORDER BY p.requestedAt DESC")
    List<PayoutRequest> findByStatusOrderByRequestedAtDesc(@Param("status") PayoutStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PayoutRequest p WHERE p.owner = :owner AND p.status IN :statuses")
    BigDecimal sumAmountByOwnerAndStatusIn(@Param("owner") User owner, @Param("statuses") List<PayoutStatus> statuses);
}
