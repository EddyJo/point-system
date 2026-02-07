package com.pointsystem.point.service;

import com.pointsystem.common.exception.BusinessException;
import com.pointsystem.common.exception.ErrorCode;
import com.pointsystem.point.controller.dto.PointGrantRequest;
import com.pointsystem.point.controller.dto.PointSpendRequest;
import com.pointsystem.point.domain.entity.*;
import com.pointsystem.point.domain.repository.PointGrantRepository;
import com.pointsystem.point.domain.repository.PointLedgerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
class PointSpendServiceTest {

    private static final String CUSTOMER_ID = "test-customer";
    @Autowired
    private PointSpendService spendService;
    @Autowired
    private PointGrantService grantService;
    @Autowired
    private PointGrantRepository grantRepository;
    @Autowired
    private PointLedgerRepository ledgerRepository;

    private PointGrant createGrant(long amount) {
        return grantService.grantPoint(
                new PointGrantRequest(CUSTOMER_ID, amount, GrantType.SYSTEM, null));
    }

    @Nested
    class createSpend_테스트 {

        @Test
        void 정상_사용시_Spend와_Allocation과_Ledger가_생성된다() {
            createGrant(1000L);

            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 500L));

            assertThat(spend.getSpendId()).isNotNull();
            assertThat(spend.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(spend.getOrderId()).isEqualTo("order-001");
            assertThat(spend.getAmountTotal()).isEqualTo(500L);
            assertThat(spend.getAmountCanceled()).isEqualTo(0L);
            assertThat(spend.getStatus()).isEqualTo(SpendStatus.USED);
            assertThat(spend.getAllocations()).hasSize(1);

            List<PointLedger> ledgers = ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID);
            PointLedger spendLedger = ledgers.get(0);
            assertThat(spendLedger.getEventType()).isEqualTo(LedgerEventType.SPEND);
            assertThat(spendLedger.getAmount()).isEqualTo(-500L);
            assertThat(spendLedger.getOrderId()).isEqualTo("order-001");
        }

        @Test
        void 사용_후_Grant의_잔액이_차감된다() {
            PointGrant grant = createGrant(1000L);

            spendService.spendPoint(new PointSpendRequest(CUSTOMER_ID, "order-001", 600L));

            PointGrant updated = grantRepository.findById(grant.getGrantId()).orElseThrow();
            assertThat(updated.getAmountAvailable()).isEqualTo(400L);
        }

        @Test
        void 복수_Grant에서_만료_임박순으로_차감된다() {
            PointGrant grantA = createGrant(1000L);
            PointGrant grantB = createGrant(500L);

            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 1200L));

            assertThat(spend.getAllocations()).hasSize(2);

            long totalAllocated = spend.getAllocations().stream()
                    .mapToLong(PointSpendAllocation::getAmountUsed)
                    .sum();
            assertThat(totalAllocated).isEqualTo(1200L);
        }

        @Test
        void 잔액_전부를_사용할_수_있다() {
            createGrant(1000L);

            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 1000L));

            assertThat(spend.getAmountTotal()).isEqualTo(1000L);

            long balance = grantRepository.calculateAvailableBalance(
                    CUSTOMER_ID, GrantStatus.ACTIVE, spend.getCreatedAt());
            assertThat(balance).isEqualTo(0L);
        }

        @Test
        void 잔액이_부족하면_예외가_발생한다() {
            createGrant(500L);

            assertThatThrownBy(() -> spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 1000L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SPEND_INSUFFICIENT_BALANCE));
        }

        @Test
        void 적립이_없으면_예외가_발생한다() {
            assertThatThrownBy(() -> spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 100L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SPEND_INSUFFICIENT_BALANCE));
        }

        @Test
        void 동일_주문번호로_중복_사용하면_예외가_발생한다() {
            createGrant(10000L);
            spendService.spendPoint(new PointSpendRequest(CUSTOMER_ID, "order-001", 100L));

            assertThatThrownBy(() -> spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 200L)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SPEND_DUPLICATE_ORDER));
        }
    }

}