package com.pointsystem.point.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "포인트 사용 요청")
public record PointSpendRequest(
        @Schema(description = "고객 식별자", example = "cust-001")
        @NotBlank(message = "고객 ID는 필수입니다.") String customerId,

        @Schema(description = "주문번호 (중복 사용 방지)", example = "order-A1234")
        @NotBlank(message = "주문 ID는 필수입니다.") String orderId,

        @Schema(description = "사용 금액", example = "1200")
        @NotNull(message = "사용 금액은 필수입니다.") @Min(value = 1, message = "사용 금액은 1원 이상이어야 합니다.") Long amount
) {
}
