package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.PointSpendAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PointSpendAllocationRepository extends JpaRepository<PointSpendAllocation, String> {
    
    @Query("SELECT a FROM PointSpendAllocation a JOIN FETCH a.grant g " +
            "WHERE a.spend.spendId = :spendId " +
            "ORDER BY CASE WHEN g.expiresAt > :now AND g.status = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "g.expiresAt ASC, a.createdAt ASC")
    List<PointSpendAllocation> findBySpendIdWithGrantForCancel(
            @Param("spendId") String spendId,
            @Param("now") Instant now);
}
