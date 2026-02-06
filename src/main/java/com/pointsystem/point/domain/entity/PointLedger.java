package com.pointsystem.point.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLedger {

    @Id
    @Column(length = 36)
    private String ledgerId;

    @Column(nullable = false, length = 50)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LedgerEventType eventType;

    @Column(length = 36)
    private String refId;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 100)
    private String orderId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private PointLedger(String customerId, LedgerEventType eventType, String refId, long amount, String orderId, Instant createdAt) {
        this.customerId = customerId;
        this.eventType = eventType;
        this.refId = refId;
        this.amount = amount;
        this.orderId = orderId;
        this.createdAt = createdAt;
    }

    public static PointLedger create(String customerId, LedgerEventType eventType, String refId, long amount, String orderId, Instant now) {
        return new PointLedger(customerId, eventType, refId, amount, orderId, now);
    }

    @PrePersist
    protected void onCreate() {
        if (this.ledgerId == null) {
            this.ledgerId = UUID.randomUUID().toString();
        }
    }
}
