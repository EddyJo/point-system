package com.pointsystem.point.controller.dto;

import com.pointsystem.point.domain.entity.SpendStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "포인트 사용 취소 응답")
public record PointSpendCancelResponse(
        @Schema(description = "사용 ID") String spendId,
        @Schema(description = "사용 상태") SpendStatus status,
        @Schema(description = "총 사용 금액") Long amountTotal,
        @Schema(description = "누적 취소 금액") Long amountCanceled,
        @Schema(description = "이번 취소 금액") Long canceledAmount,
        @Schema(description = "원래 적립건에 복원된 금액") Long restoredToOriginalGrants,
        @Schema(description = "만료로 인해 신규 적립된 금액") Long restoredAsNewGrants,
        @Schema(description = "신규 생성된 복원 적립 ID 목록") List<String> newRestoreGrantIds
) {
    public static PointSpendCancelResponse from(PointSpendCancelResult result) {
        return new PointSpendCancelResponse(
                result.spend().getSpendId(),
                result.spend().getStatus(),
                result.spend().getAmountTotal(),
                result.spend().getAmountCanceled(),
                result.canceledAmount(),
                result.restoredToOriginalGrants(),
                result.restoredAsNewGrants(),
                result.newRestoreGrants().stream()
                        .map(g -> g.getGrantId())
                        .toList()
        );
    }
}
