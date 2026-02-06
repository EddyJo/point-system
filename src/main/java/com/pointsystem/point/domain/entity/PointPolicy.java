package com.pointsystem.point.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "point_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointPolicy {
    @Id
    @Column(length = 50)
    private String policyKey;

    @Column(nullable = false, length = 100)
    private String policyValue;

    @Column(nullable = false)
    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public PointPolicy(String policyKey, String policyValue) {
        this.policyKey = policyKey;
        this.policyValue = policyValue;
        this.updatedAt = Instant.now();
    }
}
