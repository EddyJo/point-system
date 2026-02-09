package com.pointsystem.point.controller.dto;

import com.pointsystem.point.domain.entity.PointSpend;
import com.pointsystem.point.domain.entity.SpendStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "포인트 사용 응답")
public record PointSpendResponse(
        @Schema(description = "사용 ID") String spendId,
        @Schema(description = "고객 식별자") String customerId,
        @Schema(description = "주문번호") String orderId,
        @Schema(description = "총 사용 금액") Long amountTotal,
        @Schema(description = "취소된 금액") Long amountCanceled,
        @Schema(description = "사용 상태 (USED, PARTIALLY_CANCELED, CANCELED)") SpendStatus status,
        @Schema(description = "사용일시") Instant createdAt,
        @Schema(description = "적립건별 차감 내역") List<AllocationInfo> allocations
) {
    public static PointSpendResponse from(PointSpend pointSpend) {
        List<AllocationInfo> allocations = pointSpend.getAllocations().stream()
                .map(a -> new AllocationInfo(
                        a.getAllocationId(),
                        a.getGrant().getGrantId(),
                        a.getAmountUsed(),
                        a.getAmountCanceled()))
                .toList();

        return new PointSpendResponse(
                pointSpend.getSpendId(),
                pointSpend.getCustomerId(),
                pointSpend.getOrderId(),
                pointSpend.getAmountTotal(),
                pointSpend.getAmountCanceled(),
                pointSpend.getStatus(),
                pointSpend.getCreatedAt(),
                allocations
        );
    }

    @Schema(description = "적립건별 차감 내역")
    public record AllocationInfo(
            @Schema(description = "배분 ID") String allocationId,
            @Schema(description = "적립 ID") String grantId,
            @Schema(description = "사용 금액") Long amountUsed,
            @Schema(description = "취소된 금액") Long amountCanceled
    ) {
    }
}
