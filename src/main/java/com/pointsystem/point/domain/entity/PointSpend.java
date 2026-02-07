package com.pointsystem.point.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "point_spend")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointSpend {

    @Id
    @Column(length = 36)
    private String spendId;

    @Column(nullable = false, length = 50)
    private String customerId;

    @Column(nullable = false, length = 100)
    private String orderId;

    @Column(nullable = false)
    private Long amountTotal;

    @Column(nullable = false)
    private Long amountCanceled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SpendStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "spend", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PointSpendAllocation> allocations = new ArrayList<>();

    private PointSpend(String customerId, String orderId, long amountTotal, Instant createdAt) {
        this.customerId = customerId;
        this.orderId = orderId;
        this.amountTotal = amountTotal;
        this.amountCanceled = 0L;
        this.status = SpendStatus.USED;
        this.createdAt = createdAt;
    }

    public static PointSpend create(String customerId, String orderId, long amountTotal, Instant now) {
        return new PointSpend(customerId, orderId, amountTotal, now);
    }

    @PrePersist
    protected void onCreate() {
        if (this.spendId == null) {
            this.spendId = UUID.randomUUID().toString();
        }
    }

    public void addAllocation(PointSpendAllocation allocation) {
        this.allocations.add(allocation);
        allocation.setSpend(this);
    }
}
