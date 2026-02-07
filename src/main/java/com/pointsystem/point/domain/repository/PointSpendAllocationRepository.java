package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.PointSpendAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointSpendAllocationRepository extends JpaRepository<PointSpendAllocation, String> {
    @Query("SELECT a FROM PointSpendAllocation a JOIN FETCH a.grant WHERE a.spend.spendId = :spendId ORDER BY a.createdAt ASC")
    List<PointSpendAllocation> findBySpendIdWithGrant(@Param("spendId") String spendId);
}
