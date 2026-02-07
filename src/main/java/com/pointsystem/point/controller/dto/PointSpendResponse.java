package com.pointsystem.point.controller.dto;

import com.pointsystem.point.domain.entity.PointSpend;
import com.pointsystem.point.domain.entity.SpendStatus;

import java.time.Instant;
import java.util.List;

public record PointSpendResponse(
        String spendId,
        String customerId,
        String orderId,
        Long amountTotal,
        Long amountCanceled,
        SpendStatus status,
        Instant createdAt,
        List<AllocationInfo> allocations
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

    public record AllocationInfo(
            String allocationId,
            String grantId,
            Long amountUsed,
            Long amountCanceled
    ) {
    }
}
