package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.PointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, String> {
}
