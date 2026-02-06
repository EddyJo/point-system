package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.GrantStatus;
import com.pointsystem.point.domain.entity.PointGrant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PointGrantRepository extends JpaRepository<PointGrant, String> {
    @Query("SELECT COALESCE(SUM(g.amountAvailable), 0) FROM PointGrant g " +
            "WHERE g.customerId = :customerId AND g.status = :status AND g.expiresAt > :now")
    Long calculateAvailableBalance(
            @Param("customerId") String customerId,
            @Param("status") GrantStatus status,
            @Param("now") Instant now
    );
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM PointGrant g WHERE g.grantId = :grantId")
    Optional<PointGrant> findByIdWithLock(@Param("grantId") String grantId);

}
