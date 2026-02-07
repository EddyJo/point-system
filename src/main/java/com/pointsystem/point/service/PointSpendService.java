package com.pointsystem.point.service;

import com.pointsystem.common.exception.BusinessException;
import com.pointsystem.common.exception.ErrorCode;
import com.pointsystem.point.controller.dto.PointSpendRequest;
import com.pointsystem.point.domain.entity.*;
import com.pointsystem.point.domain.repository.PointGrantRepository;
import com.pointsystem.point.domain.repository.PointLedgerRepository;
import com.pointsystem.point.domain.repository.PointSpendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PointSpendService {
    private final PointGrantRepository grantRepository;
    private final PointSpendRepository spendRepository;
    private final PointLedgerRepository ledgerRepository;

    @Transactional
    public PointSpend spendPoint(PointSpendRequest request) {
        Instant now = Instant.now();
        log.info("포인트 사용 요청: customerId={}, amount={}, orderId={}", request.customerId(), request.amount(), request.orderId());

        if (spendRepository.existsByCustomerIdAndOrderId(request.customerId(), request.orderId())) {
            throw new BusinessException(ErrorCode.SPEND_DUPLICATE_ORDER);
        }

        //TODO PointGrantService에서 해당 기능 제공하기.
        //TODO usableGrants -> 일급컬랙션으로 리팩토링(sumAmountAvailable, deductSpendAmount)
        List<PointGrant> usableGrants = grantRepository.findUsableGrantsWithLock(
                request.customerId(), GrantStatus.ACTIVE, now);

        long totalAvailable = usableGrants.stream()
                .mapToLong(PointGrant::getAmountAvailable)
                .sum();

        if (totalAvailable < request.amount()) {
            throw new BusinessException(ErrorCode.SPEND_INSUFFICIENT_BALANCE,
                    String.format("(필요: %d, 보유: %d)", request.amount(), totalAvailable));
        }

        PointSpend spend = PointSpend.create(
                request.customerId(),
                request.orderId(),
                request.amount(),
                now
        );

        long remainingAmount = request.amount();
        for (PointGrant grant : usableGrants) {
            if (remainingAmount <= 0) break;

            long deductAmount = grant.debit(remainingAmount);
            if (deductAmount > 0) {
                PointSpendAllocation allocation = PointSpendAllocation.create(grant, deductAmount, now);
                spend.addAllocation(allocation);
                remainingAmount -= deductAmount;
            }
        }

        spendRepository.save(spend);
        recordLedger(spend.getCustomerId(), LedgerEventType.SPEND, spend.getSpendId(),
                -spend.getAmountTotal(), spend.getOrderId(), now);

        log.info("포인트 사용 완료: spendId={}, customerId={}, amount={}, orderId={}", spend.getSpendId(), spend.getCustomerId(), spend.getAmountTotal(), spend.getOrderId());
        return spend;
    }

    private void recordLedger(String customerId, LedgerEventType eventType, String refId, long amount, String orderId, Instant now) {
        PointLedger ledger = PointLedger.create(customerId, eventType, refId, amount, orderId, now);
        ledgerRepository.save(ledger);
    }
}
