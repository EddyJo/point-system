package com.pointsystem.point.controller.dto;

import com.pointsystem.point.domain.entity.GrantType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PointGrantRequest(
        @NotBlank(message = "고객 ID는 필수입니다.") String customerId,
        @NotNull(message = "적립 금액은 필수입니다.") @Min(value = 1, message = "적립 금액은 1원 이상이어야 합니다.") Long amount,
        @NotNull(message = "적립 타입은 필수입니다.") GrantType grantType,
        Instant expiresAt
) {
}
