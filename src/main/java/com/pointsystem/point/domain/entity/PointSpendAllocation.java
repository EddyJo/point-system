package com.pointsystem.point.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_spend_allocation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointSpendAllocation {

    @Id
    @Column(length = 36)
    private String allocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spend_id", nullable = false)
    private PointSpend spend;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grant_id", nullable = false)
    private PointGrant grant;

    @Column(nullable = false)
    private Long amountUsed;

    @Column(nullable = false)
    private Long amountCanceled;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private PointSpendAllocation(PointGrant grant, long usedAmount, Instant createdAt) {
        this.grant = grant;
        this.amountUsed = usedAmount;
        this.amountCanceled = 0L;
        this.createdAt = createdAt;
    }

    public static PointSpendAllocation create(PointGrant grant, long usedAmount, Instant now) {
        return new PointSpendAllocation(grant, usedAmount, now);
    }

    @PrePersist
    protected void onCreate() {
        if (this.allocationId == null) {
            this.allocationId = UUID.randomUUID().toString();
        }
    }

    void setSpend(PointSpend spend) {
        this.spend = spend;
    }

    public long remainingCancelable() {
        return this.amountUsed - this.amountCanceled;
    }

    public long cancelAsPossible(long requestAmount) {
        long toCancel = Math.min(remainingCancelable(), requestAmount);
        this.amountCanceled += toCancel;
        return toCancel;
    }
}
