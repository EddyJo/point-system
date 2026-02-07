package com.pointsystem.point.controller.dto;

import com.pointsystem.point.domain.entity.PointGrant;
import com.pointsystem.point.domain.entity.PointSpend;

import java.util.List;

public record PointSpendCancelResult(
        PointSpend spend,
        long canceledAmount,
        long restoredToOriginalGrants,
        long restoredAsNewGrants,
        List<PointGrant> newRestoreGrants
) {
    public static PointSpendCancelResult of(PointSpend spend, long canceledAmount, long restoredToOriginalGrants,
                                            long restoredAsNewGrants, List<PointGrant> newRestoreGrants) {
        return new PointSpendCancelResult(spend, canceledAmount, restoredToOriginalGrants, restoredAsNewGrants, newRestoreGrants);
    }
}
