package com.pointsystem.point.service;

import com.pointsystem.common.exception.BusinessException;
import com.pointsystem.common.exception.ErrorCode;
import com.pointsystem.point.controller.dto.PointSpendCancelResult;
import com.pointsystem.point.controller.dto.PointSpendRequest;
import com.pointsystem.point.domain.entity.*;
import com.pointsystem.point.domain.repository.PointGrantRepository;
import com.pointsystem.point.domain.repository.PointLedgerRepository;
import com.pointsystem.point.domain.repository.PointSpendAllocationRepository;
import com.pointsystem.point.domain.repository.PointSpendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PointSpendService {
    private final PointGrantRepository grantRepository;
    private final PointSpendRepository spendRepository;
    private final PointLedgerRepository ledgerRepository;
    private final PointGrantService grantService;
    private final PointSpendAllocationRepository allocationRepository;

    @Transactional
    public PointSpend spendPoint(PointSpendRequest request) {
        Instant now = Instant.now();
        log.info("포인트 사용 요청: customerId={}, amount={}, orderId={}", request.customerId(), request.amount(), request.orderId());

        if (spendRepository.existsByCustomerIdAndOrderId(request.customerId(), request.orderId())) {
            throw new BusinessException(ErrorCode.SPEND_DUPLICATE_ORDER);
        }

        UsableGrants usableGrants = grantService.findUsableGrantsWithLock(request.customerId(), now);

        usableGrants.validateSufficientBalance(request.amount());

        PointSpend spend = PointSpend.create(
                request.customerId(),
                request.orderId(),
                request.amount(),
                now
        );

        List<PointSpendAllocation> allocations = usableGrants.deduct(request.amount(), now);
        allocations.forEach(spend::addAllocation);

        spendRepository.save(spend);
        recordLedger(spend.getCustomerId(), LedgerEventType.SPEND, spend.getSpendId(),
                -spend.getAmountTotal(), spend.getOrderId(), now);

        log.info("포인트 사용 완료: spendId={}, customerId={}, amount={}, orderId={}", spend.getSpendId(), spend.getCustomerId(), spend.getAmountTotal(), spend.getOrderId());
        return spend;
    }

    public PointSpendCancelResult cancelSpend(String spendId, long cancelAmount) {
        Instant now = Instant.now();
        log.info("포인트 사용 취소 요청: spendId={}, cancelAmount={}", spendId, cancelAmount);

        PointSpend spend = spendRepository.findByIdWithLock(spendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPEND_NOT_FOUND));

        if (spend.getStatus() == SpendStatus.CANCELED) {
            throw new BusinessException(ErrorCode.SPEND_ALREADY_CANCELED);
        }

        long cancelable = spend.cancellableAmount();
        if (cancelAmount < 1 || cancelAmount > cancelable) {
            throw new BusinessException(ErrorCode.SPEND_CANCEL_AMOUNT_INVALID,
                    String.format("(취소 가능: %d, 요청: %d)", cancelable, cancelAmount));
        }
        //TODO PointAllocationService 해당 기능 제공하기.
        //TODO allocations -> 일급컬랙션으로 리팩토링(취소 처리, deductSpendAmount)
        List<PointSpendAllocation> allocations = allocationRepository.findBySpendIdWithGrantForCancel(spendId, now);

        long remainingCancel = cancelAmount;
        long restoredToOriginal = 0;
        long restoredAsNew = 0;
        List<PointGrant> newRestoreGrants = new ArrayList<>();

        for (PointSpendAllocation allocation : allocations) {
            if (remainingCancel <= 0) break;

            long toCancel = allocation.cancelAsPossible(remainingCancel);
            if (toCancel <= 0) continue;

            PointGrant originalGrant = allocation.getGrant();

            if (!originalGrant.isExpired(now) && originalGrant.isActive()) {
                originalGrant.credit(toCancel);
                restoredToOriginal += toCancel;
            } else {
                PointGrant restoreGrant = grantService.restoreGrant(spend.getCustomerId(), toCancel, now);
                newRestoreGrants.add(restoreGrant);
                restoredAsNew += toCancel;
            }

            remainingCancel -= toCancel;
        }

        spend.applyCancel(cancelAmount);
        recordLedger(spend.getCustomerId(), LedgerEventType.SPEND_CANCEL, spend.getSpendId(),
                cancelAmount, spend.getOrderId(), now);

        log.info("포인트 사용 취소 완료: spendId={}, cancelAmount={}, restoredToOriginal={}, restoredAsNew={}", spendId, cancelAmount, restoredToOriginal, restoredAsNew);
        return PointSpendCancelResult.of(spend, cancelAmount, restoredToOriginal, restoredAsNew, newRestoreGrants);
    }

    private void recordLedger(String customerId, LedgerEventType eventType, String refId, long amount, String orderId, Instant now) {
        PointLedger ledger = PointLedger.create(customerId, eventType, refId, amount, orderId, now);
        ledgerRepository.save(ledger);
    }
}
