package com.pointsystem.point.domain.entity;

import com.pointsystem.common.exception.BusinessException;
import com.pointsystem.common.exception.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsableGrantsTest {

    private static final String CUSTOMER_ID = "test-customer";
    private static final Instant NOW = Instant.now();
    private static final Instant EXPIRES_AT = NOW.plus(30, ChronoUnit.DAYS);

    private PointGrant createGrant(long amount) {
        return PointGrant.create(CUSTOMER_ID, GrantType.MANUAL, amount, EXPIRES_AT, NOW);
    }

    @Nested
    class totalAvailable_테스트 {

        @Test
        void 적립건들의_잔액_합계를_반환한다() {
            UsableGrants usableGrants = new UsableGrants(List.of(
                    createGrant(1000),
                    createGrant(2000),
                    createGrant(3000)
            ));

            assertThat(usableGrants.totalAvailable()).isEqualTo(6000L);
        }

        @Test
        void 적립건이_없으면_0을_반환한다() {
            UsableGrants usableGrants = new UsableGrants(List.of());

            assertThat(usableGrants.totalAvailable()).isEqualTo(0L);
        }

        @Test
        void 일부_사용된_적립건의_잔액만_합산한다() {
            PointGrant grant1 = createGrant(1000);
            grant1.debit(300);
            PointGrant grant2 = createGrant(2000);

            UsableGrants usableGrants = new UsableGrants(List.of(grant1, grant2));

            assertThat(usableGrants.totalAvailable()).isEqualTo(2700L);
        }
    }

    @Nested
    class validateSufficientBalance_테스트 {

        @Test
        void 잔액이_충분하면_예외가_발생하지_않는다() {
            UsableGrants usableGrants = new UsableGrants(List.of(
                    createGrant(3000),
                    createGrant(2000)
            ));

            usableGrants.validateSufficientBalance(5000);
        }

        @Test
        void 잔액이_부족하면_SPEND_INSUFFICIENT_BALANCE_예외가_발생한다() {
            UsableGrants usableGrants = new UsableGrants(List.of(
                    createGrant(1000)
            ));

            assertThatThrownBy(() -> usableGrants.validateSufficientBalance(1001))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SPEND_INSUFFICIENT_BALANCE));
        }

        @Test
        void 잔액과_요청금액이_동일하면_예외가_발생하지_않는다() {
            UsableGrants usableGrants = new UsableGrants(List.of(
                    createGrant(5000)
            ));

            usableGrants.validateSufficientBalance(5000);
        }

        @Test
        void 적립건이_없으면_예외가_발생한다() {
            UsableGrants usableGrants = new UsableGrants(List.of());

            assertThatThrownBy(() -> usableGrants.validateSufficientBalance(1))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SPEND_INSUFFICIENT_BALANCE));
        }
    }

    @Nested
    class deduct_테스트 {

        @Test
        void 단일_적립건에서_전액_차감한다() {
            PointGrant grant = createGrant(5000);
            UsableGrants usableGrants = new UsableGrants(List.of(grant));

            List<PointSpendAllocation> allocations = usableGrants.deduct(3000, NOW);

            assertThat(allocations).hasSize(1);
            assertThat(allocations.get(0).getAmountUsed()).isEqualTo(3000L);
            assertThat(grant.getAmountAvailable()).isEqualTo(2000L);
        }

        @Test
        void 여러_적립건에_걸쳐_순서대로_차감한다() {
            PointGrant grant1 = createGrant(1000);
            PointGrant grant2 = createGrant(2000);
            PointGrant grant3 = createGrant(3000);
            UsableGrants usableGrants = new UsableGrants(List.of(grant1, grant2, grant3));

            List<PointSpendAllocation> allocations = usableGrants.deduct(2500, NOW);

            assertThat(allocations).hasSize(2);
            assertThat(allocations.get(0).getAmountUsed()).isEqualTo(1000L);
            assertThat(allocations.get(1).getAmountUsed()).isEqualTo(1500L);
            assertThat(grant1.getAmountAvailable()).isEqualTo(0L);
            assertThat(grant2.getAmountAvailable()).isEqualTo(500L);
            assertThat(grant3.getAmountAvailable()).isEqualTo(3000L);
        }

        @Test
        void 차감금액이_0이면_빈_목록을_반환한다() {
            UsableGrants usableGrants = new UsableGrants(List.of(createGrant(1000)));

            List<PointSpendAllocation> allocations = usableGrants.deduct(0, NOW);

            assertThat(allocations).isEmpty();
        }

        @Test
        void 모든_적립건을_전액_소진한다() {
            PointGrant grant1 = createGrant(1000);
            PointGrant grant2 = createGrant(2000);
            UsableGrants usableGrants = new UsableGrants(List.of(grant1, grant2));

            List<PointSpendAllocation> allocations = usableGrants.deduct(3000, NOW);

            assertThat(allocations).hasSize(2);
            assertThat(allocations.get(0).getAmountUsed()).isEqualTo(1000L);
            assertThat(allocations.get(1).getAmountUsed()).isEqualTo(2000L);
            assertThat(grant1.getAmountAvailable()).isEqualTo(0L);
            assertThat(grant2.getAmountAvailable()).isEqualTo(0L);
        }

        @Test
        void 잔액이_0인_적립건은_건너뛴다() {
            PointGrant emptyGrant = createGrant(1000);
            emptyGrant.debit(1000);
            PointGrant activeGrant = createGrant(2000);
            UsableGrants usableGrants = new UsableGrants(List.of(emptyGrant, activeGrant));

            List<PointSpendAllocation> allocations = usableGrants.deduct(500, NOW);

            assertThat(allocations).hasSize(1);
            assertThat(allocations.get(0).getAmountUsed()).isEqualTo(500L);
            assertThat(activeGrant.getAmountAvailable()).isEqualTo(1500L);
        }

        @Test
        void 각_allocation의_grant가_올바르게_연결된다() {
            PointGrant grant1 = createGrant(500);
            PointGrant grant2 = createGrant(1000);
            UsableGrants usableGrants = new UsableGrants(List.of(grant1, grant2));

            List<PointSpendAllocation> allocations = usableGrants.deduct(800, NOW);

            assertThat(allocations.get(0).getGrant()).isSameAs(grant1);
            assertThat(allocations.get(1).getGrant()).isSameAs(grant2);
        }
    }
}
