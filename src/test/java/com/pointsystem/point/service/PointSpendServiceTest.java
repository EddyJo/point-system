package com.pointsystem.point.service;

import com.pointsystem.common.exception.BusinessException;
import com.pointsystem.common.exception.ErrorCode;
import com.pointsystem.point.controller.dto.PointGrantRequest;
import com.pointsystem.point.controller.dto.PointSpendCancelResult;
import com.pointsystem.point.controller.dto.PointSpendRequest;
import com.pointsystem.point.domain.entity.*;
import com.pointsystem.point.domain.repository.PointGrantRepository;
import com.pointsystem.point.domain.repository.PointLedgerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    @Autowired
    private EntityManager entityManager;

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

    @Nested
    class cancelSpend_테스트 {

        @Test
        void 전액_취소시_상태가_CANCELED가_된다() {
            createGrant(1000L);
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 500L));

            PointSpendCancelResult result = spendService.cancelSpend(spend.getSpendId(), 500L);

            assertThat(result.spend().getStatus()).isEqualTo(SpendStatus.CANCELED);
            assertThat(result.spend().getAmountCanceled()).isEqualTo(500L);
            assertThat(result.canceledAmount()).isEqualTo(500L);
        }

        @Test
        void 부분_취소시_상태가_PARTIALLY_CANCELED가_된다() {
            createGrant(1000L);
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 500L));

            PointSpendCancelResult result = spendService.cancelSpend(spend.getSpendId(), 200L);

            assertThat(result.spend().getStatus()).isEqualTo(SpendStatus.PARTIALLY_CANCELED);
            assertThat(result.spend().getAmountCanceled()).isEqualTo(200L);
            assertThat(result.spend().cancellableAmount()).isEqualTo(300L);
        }

        @Test
        void 취소시_원래_Grant에_잔액이_복원된다() {
            PointGrant grant = createGrant(1000L);
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 600L));

            spendService.cancelSpend(spend.getSpendId(), 400L);

            PointGrant updated = grantRepository.findById(grant.getGrantId()).orElseThrow();
            assertThat(updated.getAmountAvailable()).isEqualTo(800L);
        }

        @Test
        void 취소시_Ledger에_SPEND_CANCEL이_기록된다() {
            createGrant(1000L);
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 500L));

            spendService.cancelSpend(spend.getSpendId(), 300L);

            List<PointLedger> ledgers = ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID);
            PointLedger cancelLedger = ledgers.get(0);
            assertThat(cancelLedger.getEventType()).isEqualTo(LedgerEventType.SPEND_CANCEL);
            assertThat(cancelLedger.getAmount()).isEqualTo(300L);
        }

        @Test
        void 부분_취소_후_추가_부분_취소가_가능하다() {
            createGrant(1000L);
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 500L));

            spendService.cancelSpend(spend.getSpendId(), 200L);
            PointSpendCancelResult result = spendService.cancelSpend(spend.getSpendId(), 300L);

            assertThat(result.spend().getStatus()).isEqualTo(SpendStatus.CANCELED);
            assertThat(result.spend().getAmountCanceled()).isEqualTo(500L);
        }

        @Test
        void 복수_Grant_사용_후_취소시_잔액이_올바르게_복원된다() {
            PointGrant grantA = createGrant(1000L);
            PointGrant grantB = createGrant(1000L);
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 1500L));

            spendService.cancelSpend(spend.getSpendId(), 1500L);

            long totalBalance = grantRepository.calculateAvailableBalance(
                    CUSTOMER_ID, GrantStatus.ACTIVE, spend.getCreatedAt());
            assertThat(totalBalance).isEqualTo(2000L);
        }

        @Test
        void 만료된_Grant의_사용_취소시_RESTORE_Grant가_발급된다() {

            PointGrant grant = grantService.grantPoint(
                    new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, null));

            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 500L));

            //만료일을 과거로 강제 변경
            entityManager.createNativeQuery(
                            "UPDATE point_grant SET expires_at = :expiredAt WHERE grant_id = :grantId")
                    .setParameter("expiredAt", Instant.now().minus(1, ChronoUnit.DAYS))
                    .setParameter("grantId", grant.getGrantId())
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();

            PointSpendCancelResult result = spendService.cancelSpend(spend.getSpendId(), 500L);

            assertThat(result.restoredAsNewGrants()).isEqualTo(500L);
            assertThat(result.restoredToOriginalGrants()).isEqualTo(0L);
            assertThat(result.newRestoreGrants()).hasSize(1);

            PointGrant restoreGrant = result.newRestoreGrants().get(0);
            assertThat(restoreGrant.getGrantType()).isEqualTo(GrantType.RESTORE);
            assertThat(restoreGrant.getAmountTotal()).isEqualTo(500L);
            assertThat(restoreGrant.getCustomerId()).isEqualTo(CUSTOMER_ID);

            // Ledger에 RESTORE_GRANT 기록 확인
            List<PointLedger> ledgers = ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID);
            boolean hasRestoreLedger = ledgers.stream()
                    .anyMatch(i -> i.getEventType() == LedgerEventType.RESTORE_GRANT && i.getAmount() == 500L);
            assertThat(hasRestoreLedger).isTrue();
        }

        @Test
        void 부분_취소시_유효한_적립부터_우선_복원된다() {
            // Grant A: 만료 예정 → 사용 후 만료시킴
            // Grant B: 유효함
            PointGrant grantA = grantService.grantPoint(
                    new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, null));
            PointGrant grantB = grantService.grantPoint(
                    new PointGrantRequest(CUSTOMER_ID, 500L, GrantType.SYSTEM, null));

            // 1200원 사용 → A에서 1000, B에서 200 차감
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 1200L));

            // Grant A를 만료시킴
            entityManager.createNativeQuery(
                            "UPDATE point_grant SET expires_at = :expiredAt WHERE grant_id = :grantId")
                    .setParameter("expiredAt", Instant.now().minus(1, ChronoUnit.DAYS))
                    .setParameter("grantId", grantA.getGrantId())
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();

            // 500원 부분 취소
            PointSpendCancelResult result = spendService.cancelSpend(spend.getSpendId(), 500L);

            // 유효한 Grant B부터 복원되어야 한다 (200원)
            // 나머지 300원은 만료된 Grant A → RESTORE 신규 적립
            assertThat(result.restoredToOriginalGrants()).isEqualTo(200L);
            assertThat(result.restoredAsNewGrants()).isEqualTo(300L);
            assertThat(result.newRestoreGrants()).hasSize(1);
            assertThat(result.newRestoreGrants().get(0).getGrantType()).isEqualTo(GrantType.RESTORE);
            assertThat(result.newRestoreGrants().get(0).getAmountTotal()).isEqualTo(300L);

            // Grant B 잔액 확인: 원래 500 - 200 사용 + 200 복원 = 500
            PointGrant updatedB = grantRepository.findById(grantB.getGrantId()).orElseThrow();
            assertThat(updatedB.getAmountAvailable()).isEqualTo(500L);
        }

        @Test
        void 존재하지_않는_spendId로_취소하면_예외가_발생한다() {
            assertThatThrownBy(() -> spendService.cancelSpend("non-existent", 100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SPEND_NOT_FOUND));
        }

        @Test
        void 이미_전액_취소된_Spend를_다시_취소하면_예외가_발생한다() {
            createGrant(1000L);
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 500L));
            spendService.cancelSpend(spend.getSpendId(), 500L);

            assertThatThrownBy(() -> spendService.cancelSpend(spend.getSpendId(), 100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SPEND_ALREADY_CANCELED));
        }

        @Test
        void 취소_가능_금액을_초과하면_예외가_발생한다() {
            createGrant(1000L);
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 500L));

            assertThatThrownBy(() -> spendService.cancelSpend(spend.getSpendId(), 600L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SPEND_CANCEL_AMOUNT_INVALID));
        }

        @Test
        void 취소_금액이_0이면_예외가_발생한다() {
            createGrant(1000L);
            PointSpend spend = spendService.spendPoint(
                    new PointSpendRequest(CUSTOMER_ID, "order-001", 500L));

            assertThatThrownBy(() -> spendService.cancelSpend(spend.getSpendId(), 0L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SPEND_CANCEL_AMOUNT_INVALID));
        }
    }

}