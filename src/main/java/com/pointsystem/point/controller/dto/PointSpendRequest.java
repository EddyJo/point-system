package com.pointsystem.point.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PointSpendRequest(
        @NotBlank(message = "고객 ID는 필수입니다.") String customerId,
        @NotBlank(message = "주문 ID는 필수입니다.") String orderId,
        @NotNull(message = "사용 금액은 필수입니다.") @Min(value = 1, message = "사용 금액은 1원 이상이어야 합니다.") Long amount
) {
}
