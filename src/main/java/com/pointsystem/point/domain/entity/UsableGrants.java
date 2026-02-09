package com.pointsystem.point.domain.entity;

import com.pointsystem.common.exception.BusinessException;
import com.pointsystem.common.exception.ErrorCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UsableGrants {

    private final List<PointGrant> grants;

    public UsableGrants(List<PointGrant> grants) {
        this.grants = grants;
    }

    public long totalAvailable() {
        return grants.stream()
                .mapToLong(PointGrant::getAmountAvailable)
                .sum();
    }

    public void validateSufficientBalance(long requiredAmount) {
        long totalAvailable = totalAvailable();
        if (totalAvailable < requiredAmount) {
            throw new BusinessException(ErrorCode.SPEND_INSUFFICIENT_BALANCE,
                    String.format("(필요: %d, 보유: %d)", requiredAmount, totalAvailable));
        }
    }

    public List<PointSpendAllocation> deduct(long amount, Instant now) {
        List<PointSpendAllocation> allocations = new ArrayList<>();
        long remaining = amount;

        for (PointGrant grant : grants) {
            if (remaining <= 0) break;

            long deducted = grant.debit(remaining);
            if (deducted > 0) {
                allocations.add(PointSpendAllocation.create(grant, deducted, now));
                remaining -= deducted;
            }
        }

        return allocations;
    }
}
