package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.PointSpend;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointSpendRepository extends JpaRepository<PointSpend, String> {
    boolean existsByCustomerIdAndOrderId(String customerId, String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PointSpend s WHERE s.spendId = :spendId")
    Optional<PointSpend> findByIdWithLock(@Param("spendId") String spendId);
}
