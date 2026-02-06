package com.pointsystem.point.domain.repository;

import com.pointsystem.point.domain.entity.PointGrant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointGrantRepository extends JpaRepository<PointGrant, String> {
}
