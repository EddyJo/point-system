package com.pointsystem.point.controller.dto;

import com.pointsystem.point.domain.entity.GrantStatus;
import com.pointsystem.point.domain.entity.GrantType;
import com.pointsystem.point.domain.entity.PointGrant;

import java.time.Instant;

public record PointGrantResponse(
        String grantId,
        String customerId,
        GrantType grantType,
        Long amountTotal,
        Long amountAvailable,
        Instant expiresAt,
        GrantStatus status,
        Instant createdAt
) {
    public static PointGrantResponse from(PointGrant grant) {
        return new PointGrantResponse(
                grant.getGrantId(),
                grant.getCustomerId(),
                grant.getGrantType(),
                grant.getAmountTotal(),
                grant.getAmountAvailable(),
                grant.getExpiresAt(),
                grant.getStatus(),
                grant.getCreatedAt()
        );
    }
}
