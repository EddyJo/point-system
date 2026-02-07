package com.pointsystem.point.service;

import com.pointsystem.common.exception.BusinessException;
import com.pointsystem.common.exception.ErrorCode;
import com.pointsystem.point.controller.dto.PointGrantRequest;
import com.pointsystem.point.domain.entity.*;
import com.pointsystem.point.domain.repository.PointGrantRepository;
import com.pointsystem.point.domain.repository.PointLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PointGrantService {

    private static final long MAX_EXPIRE_YEARS = 5;
    private static final long MIN_EXPIRE_DAYS = 1;

    private final PointGrantRepository grantRepository;
    private final PointLedgerRepository ledgerRepository;
    private final PointPolicyService policyService;

    @Transactional
    public PointGrant grantPoint(PointGrantRequest request) {
        Instant now = Instant.now();
        log.info("포인트 적립 요청: customerId={}, amount={}, type={}", request.customerId(), request.amount(), request.grantType());

        validateAmount(request.amount());
        Instant expiresAt = resolveExpiresAt(request.expiresAt(), now);
        validateBalanceLimit(request.customerId(), request.amount(), now);

        PointGrant grant = PointGrant.create(
                request.customerId(),
                request.grantType(),
                request.amount(),
                expiresAt,
                now
        );

        grantRepository.save(grant);
        recordLedger(grant.getCustomerId(), LedgerEventType.GRANT, grant.getGrantId(), grant.getAmountTotal(), now);

        log.info("포인트 적립 완료: grantId={}, customerId={}, amount={}", grant.getGrantId(), grant.getCustomerId(), grant.getAmountTotal());
        return grant;
    }

    @Transactional
    public PointGrant cancelPointGrant(String grantId) {
        Instant now = Instant.now();
        log.info("포인트 적립 취소 요청: grantId={}", grantId);

        PointGrant grant = grantRepository.findByIdWithLock(grantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GRANT_NOT_FOUND));

        if (!grant.isActive()) {
            throw new BusinessException(ErrorCode.GRANT_ALREADY_CANCELED);
        }

        if (!grant.isCancelable()) {
            throw new BusinessException(ErrorCode.GRANT_ALREADY_USED);
        }

        long canceledAmount = grant.getAmountTotal();
        grant.cancel();
        recordLedger(grant.getCustomerId(), LedgerEventType.GRANT_CANCEL, grant.getGrantId(), -canceledAmount, now);

        log.info("포인트 적립 취소 완료: grantId={}, customerId={}, canceledAmount={}", grantId, grant.getCustomerId(), canceledAmount);
        return grant;
    }

    @Transactional
    public PointGrant restoreGrant(String customerId, long amount, Instant now) {
        log.info("복원 포인트 부여: customerId={}, amount={}", customerId, amount);
        long defaultExpireDays = policyService.getDefaultExpireDays();
        Instant expiresAt = now.plus(defaultExpireDays, ChronoUnit.DAYS);

        PointGrant grant = PointGrant.create(
                customerId,
                GrantType.RESTORE,
                amount,
                expiresAt,
                now
        );

        grantRepository.save(grant);
        recordLedger(customerId, LedgerEventType.RESTORE_GRANT, grant.getGrantId(), amount, now);

        log.info("복원 포인트 부여 완료: grantId={}, customerId={}, amount={}", grant.getGrantId(), customerId, amount);
        return grant;
    }

    private void validateAmount(long amount) {
        long maxGrantPerTransaction = policyService.getMaxGrantPerTransaction();
        if (amount < 1 || amount > maxGrantPerTransaction) {
            throw new BusinessException(ErrorCode.GRANT_AMOUNT_OUT_OF_RANGE,
                    String.format("(허용: 1~%d, 요청: %d)", maxGrantPerTransaction, amount));
        }
    }

    private Instant resolveExpiresAt(Instant requested, Instant now) {
        if (requested == null) {
            long defaultDays = policyService.getDefaultExpireDays();
            return now.plus(defaultDays, ChronoUnit.DAYS);
        }

        Instant minExpires = now.plus(MIN_EXPIRE_DAYS, ChronoUnit.DAYS);
        Instant maxExpires = now.plus(MAX_EXPIRE_YEARS * 365, ChronoUnit.DAYS);

        if (requested.isBefore(minExpires) || !requested.isBefore(maxExpires)) {
            throw new BusinessException(ErrorCode.GRANT_EXPIRES_AT_INVALID);
        }

        return requested;
    }

    private void validateBalanceLimit(String customerId, long addAmount, Instant now) {
        long currentBalance = grantRepository.calculateAvailableBalance(
                customerId, GrantStatus.ACTIVE, now);
        long maxBalance = policyService.getMaxBalancePerUser();
        log.debug("현재 잔액 : {}, 최대 한도 : {}", currentBalance, maxBalance);

        if (currentBalance + addAmount > maxBalance) {
            throw new BusinessException(ErrorCode.GRANT_BALANCE_LIMIT_EXCEEDED,
                    String.format("(현재: %d, 추가: %d, 한도: %d)", currentBalance, addAmount, maxBalance));
        }
    }

    private void recordLedger(String customerId, LedgerEventType eventType, String refId, long amount, Instant now) {
        PointLedger ledger = PointLedger.create(customerId, eventType, refId, amount, null, now);
        ledgerRepository.save(ledger);
    }
}
