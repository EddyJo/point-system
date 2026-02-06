package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.PointLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLedgerRepository extends JpaRepository<PointLedger, String> {
}
