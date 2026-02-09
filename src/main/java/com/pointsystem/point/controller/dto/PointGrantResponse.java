package com.pointsystem.point.controller.dto;

import com.pointsystem.point.domain.entity.GrantStatus;
import com.pointsystem.point.domain.entity.GrantType;
import com.pointsystem.point.domain.entity.PointGrant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "포인트 적립 응답")
public record PointGrantResponse(
        @Schema(description = "적립 ID") String grantId,
        @Schema(description = "고객 식별자") String customerId,
        @Schema(description = "적립 타입") GrantType grantType,
        @Schema(description = "총 적립 금액") Long amountTotal,
        @Schema(description = "사용 가능 잔액") Long amountAvailable,
        @Schema(description = "만료일시") Instant expiresAt,
        @Schema(description = "적립 상태 (ACTIVE, CANCELED)") GrantStatus status,
        @Schema(description = "적립일시") Instant createdAt
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
