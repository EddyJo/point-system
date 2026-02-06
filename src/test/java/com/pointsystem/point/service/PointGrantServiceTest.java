package com.pointsystem.point.service;

import com.pointsystem.common.exception.BusinessException;
import com.pointsystem.common.exception.ErrorCode;
import com.pointsystem.point.controller.dto.PointGrantRequest;
import com.pointsystem.point.domain.entity.*;
import com.pointsystem.point.domain.repository.PointGrantRepository;
import com.pointsystem.point.domain.repository.PointLedgerRepository;
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

@SpringBootTest
@Transactional
class PointGrantServiceTest {

    private static final String CUSTOMER_ID = "test-customer";
    @Autowired
    private PointGrantService grantService;
    @Autowired
    private PointGrantRepository grantRepository;
    @Autowired
    private PointLedgerRepository ledgerRepository;

    @Nested
    class createGrant_테스트 {

        @Test
        void 정상_적립시_Grant와_Ledger가_생성된다() {
            PointGrantRequest request = new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.MANUAL, null);

            PointGrant grant = grantService.grantPoint(request);

            assertThat(grant.getGrantId()).isNotNull();
            assertThat(grant.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(grant.getGrantType()).isEqualTo(GrantType.MANUAL);
            assertThat(grant.getAmountTotal()).isEqualTo(1000L);
            assertThat(grant.getAmountAvailable()).isEqualTo(1000L);
            assertThat(grant.getStatus()).isEqualTo(GrantStatus.ACTIVE);

            List<PointLedger> ledgers = ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID);
            assertThat(ledgers).hasSize(1);
            assertThat(ledgers.getFirst().getEventType()).isEqualTo(LedgerEventType.GRANT);
            assertThat(ledgers.getFirst().getAmount()).isEqualTo(1000L);
        }

        @Test
        void 만료일_미지정시_기본_만료일이_적용된다() {
            PointGrantRequest request = new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, null);

            PointGrant grant = grantService.grantPoint(request);

            assertThat(grant.getExpiresAt()).isAfter(Instant.now().plus(364, ChronoUnit.DAYS));
        }

        @Test
        void 만료일_지정시_해당_만료일이_적용된다() {
            Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);
            PointGrantRequest request = new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, expiresAt);

            PointGrant grant = grantService.grantPoint(request);

            assertThat(grant.getExpiresAt()).isEqualTo(expiresAt);
        }

        @Test
        void 금액이_0_이하이면_예외가_발생한다() {
            PointGrantRequest request = new PointGrantRequest(CUSTOMER_ID, 0L, GrantType.SYSTEM, null);

            assertThatThrownBy(() -> grantService.grantPoint(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GRANT_AMOUNT_OUT_OF_RANGE));
        }

        @Test
        void 금액이_건당_상한을_초과하면_예외가_발생한다() {
            PointGrantRequest request = new PointGrantRequest(CUSTOMER_ID, 100_001L, GrantType.SYSTEM, null);

            assertThatThrownBy(() -> grantService.grantPoint(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GRANT_AMOUNT_OUT_OF_RANGE));
        }

        @Test
        void 만료일이_1일_미만이면_예외가_발생한다() {
            Instant tooSoon = Instant.now().plus(12, ChronoUnit.HOURS);
            PointGrantRequest request = new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, tooSoon);

            assertThatThrownBy(() -> grantService.grantPoint(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GRANT_EXPIRES_AT_INVALID));
        }

        @Test
        void 만료일이_5년_이상이면_예외가_발생한다() {
            Instant tooLate = Instant.now().plus(5 * 365 + 1, ChronoUnit.DAYS);
            PointGrantRequest request = new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, tooLate);

            assertThatThrownBy(() -> grantService.grantPoint(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GRANT_EXPIRES_AT_INVALID));
        }

        @Test
        void 보유_한도를_초과하면_예외가_발생한다() {
            // DB 설정 한도(1,000,000)까지 적립
            for (int i = 0; i < 10; i++) {
                grantService.grantPoint(new PointGrantRequest(CUSTOMER_ID, 100_000L, GrantType.SYSTEM, null));
            }

            PointGrantRequest request = new PointGrantRequest(CUSTOMER_ID, 1L, GrantType.SYSTEM, null);

            assertThatThrownBy(() -> grantService.grantPoint(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GRANT_BALANCE_LIMIT_EXCEEDED));
        }
    }

    @Nested
    class cancelGrant_테스트 {

        @Test
        void 미사용_적립을_취소하면_상태가_CANCELED로_변경된다() {
            PointGrant grant = grantService.grantPoint(
                    new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, null));

            PointGrant canceled = grantService.cancelPointGrant(grant.getGrantId());

            assertThat(canceled.getStatus()).isEqualTo(GrantStatus.CANCELED);
            assertThat(canceled.getAmountAvailable()).isEqualTo(0L);
        }

        @Test
        void 취소시_Ledger에_GRANT_CANCEL이_기록된다() {
            PointGrant grant = grantService.grantPoint(
                    new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, null));

            grantService.cancelPointGrant(grant.getGrantId());

            List<PointLedger> ledgers = ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID);
            PointLedger cancelLedger = ledgers.getFirst();
            assertThat(cancelLedger.getEventType()).isEqualTo(LedgerEventType.GRANT_CANCEL);
            assertThat(cancelLedger.getAmount()).isEqualTo(-1000L);
            assertThat(cancelLedger.getRefId()).isEqualTo(grant.getGrantId());
        }

        @Test
        void 존재하지_않는_grantId로_취소하면_예외가_발생한다() {
            assertThatThrownBy(() -> grantService.cancelPointGrant("nothing-id"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GRANT_NOT_FOUND));
        }

        @Test
        void 이미_취소된_적립을_다시_취소하면_예외가_발생한다() {
            PointGrant grant = grantService.grantPoint(
                    new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, null));
            grantService.cancelPointGrant(grant.getGrantId());

            assertThatThrownBy(() -> grantService.cancelPointGrant(grant.getGrantId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GRANT_ALREADY_CANCELED));
        }

        @Test
        void 일부_사용된_적립을_취소하면_예외가_발생한다() {
            PointGrant grant = grantService.grantPoint(
                    new PointGrantRequest(CUSTOMER_ID, 1000L, GrantType.SYSTEM, null));

            // 일부 사용 시뮬레이션
            grant.debit(500L);
            grantRepository.save(grant);

            assertThatThrownBy(() -> grantService.cancelPointGrant(grant.getGrantId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.GRANT_ALREADY_USED));
        }
    }
}