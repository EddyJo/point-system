package com.pointsystem.point.controller.dto;

import com.pointsystem.point.domain.entity.SpendStatus;

import java.util.List;

public record PointSpendCancelResponse(
        String spendId,
        SpendStatus status,
        Long amountTotal,
        Long amountCanceled,
        Long canceledAmount,
        Long restoredToOriginalGrants,
        Long restoredAsNewGrants,
        List<String> newRestoreGrantIds
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
