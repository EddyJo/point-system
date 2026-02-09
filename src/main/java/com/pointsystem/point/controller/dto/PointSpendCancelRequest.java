package com.pointsystem.point.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "포인트 사용 취소 요청")
public record PointSpendCancelRequest(
        @Schema(description = "취소 금액", example = "1100")
        @NotNull(message = "취소 금액은 필수입니다.") @Min(value = 1, message = "취소 금액은 1원 이상이어야 합니다.") Long cancelAmount
) {
}
