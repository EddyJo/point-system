package com.pointsystem.point.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_grant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointGrant {

    @Id
    @Column(length = 36)
    private String grantId;

    @Column(nullable = false, length = 50)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GrantType grantType;

    @Column(nullable = false)
    private Long amountTotal;

    @Column(nullable = false)
    private Long amountAvailable;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GrantStatus status;

    @Column(nullable = false, updatable = false) // 생성시간 수정할 수 없음
    private Instant createdAt;

    private PointGrant(String customerId, GrantType grantType, long amount, Instant expiresAt, Instant createdAt) {
        this.customerId = customerId;
        this.grantType = grantType;
        this.amountTotal = amount;
        this.amountAvailable = amount;
        this.expiresAt = expiresAt;
        this.status = GrantStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    public static PointGrant create(String customerId, GrantType grantType, long amount, Instant expiresAt, Instant now) {
        return new PointGrant(customerId, grantType, amount, expiresAt, now);
    }

    @PrePersist
    protected void onCreate() {
        if (this.grantId == null) {
            this.grantId = UUID.randomUUID().toString();
        }
    }

    public boolean isActive() {
        return this.status == GrantStatus.ACTIVE;
    }

    public boolean isCancelable() {
        return this.status == GrantStatus.ACTIVE
                && this.amountAvailable.equals(this.amountTotal);
    }

    public void cancel() {
        if (!isCancelable()) {
            throw new IllegalStateException("취소할 수 없는 적립입니다.");
        }
        this.status = GrantStatus.CANCELED;
        this.amountAvailable = 0L;
    }

    public long debit(long amount) {
        long availableToDebit = Math.min(amount, this.amountAvailable);
        this.amountAvailable -= availableToDebit;
        return availableToDebit;
    }

    public boolean isExpired(Instant now) {
        return !this.expiresAt.isAfter(now);
    }
    
    public void credit(long amount) {
        this.amountAvailable += amount;
    }

}
