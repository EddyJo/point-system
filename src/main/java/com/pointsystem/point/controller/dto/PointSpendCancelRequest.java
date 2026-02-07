package com.pointsystem.point.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointSpendCancelRequest(
        @NotNull(message = "취소 금액은 필수입니다.") @Min(value = 1, message = "취소 금액은 1원 이상이어야 합니다.") Long cancelAmount
) {
}
