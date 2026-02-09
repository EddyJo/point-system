package com.pointsystem.point.controller.dto;

import com.pointsystem.point.domain.entity.GrantType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "포인트 적립 요청")
public record PointGrantRequest(
        @Schema(description = "고객 식별자", example = "cust-001")
        @NotBlank(message = "고객 ID는 필수입니다.") String customerId,

        @Schema(description = "적립 금액 (1 ~ 정책 상한)", example = "1000")
        @NotNull(message = "적립 금액은 필수입니다.") @Min(value = 1, message = "적립 금액은 1원 이상이어야 합니다.") Long amount,

        @Schema(description = "적립 타입 (MANUAL: 수기지급, SYSTEM: 시스템지급)", example = "MANUAL")
        @NotNull(message = "적립 타입은 필수입니다.") GrantType grantType,

        @Schema(description = "만료일시 (미입력 시 기본 365일)", example = "2025-12-31T23:59:59Z")
        Instant expiresAt
) {
}
