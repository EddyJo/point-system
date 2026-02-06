package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.PointLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointLedgerRepository extends JpaRepository<PointLedger, String> {

    List<PointLedger> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
