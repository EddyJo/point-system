package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.PointSpendAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointSpendAllocationRepository extends JpaRepository<PointSpendAllocation, String> {
}
