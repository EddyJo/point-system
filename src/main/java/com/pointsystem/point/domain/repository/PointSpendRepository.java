package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.PointSpend;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointSpendRepository extends JpaRepository<PointSpend, String> {
    boolean existsByCustomerIdAndOrderId(String customerId, String orderId);
}
