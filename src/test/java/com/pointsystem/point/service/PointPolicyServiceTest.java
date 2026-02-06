package com.pointsystem.point.service;

import com.pointsystem.point.domain.entity.PointPolicy;
import com.pointsystem.point.domain.repository.PointPolicyRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PointPolicyServiceTest {

    @Autowired
    private PointPolicyService policyService;

    @Autowired
    private PointPolicyRepository policyRepository;

    @Nested
    class getMaxGrantPerTransaction_테스트 {

        @Test
        void DB에_값이_있으면_해당_값을_반환한다() {
            policyRepository.save(new PointPolicy("MAX_GRANT_PER_TRANSACTION", "50000"));

            long result = policyService.getMaxGrantPerTransaction();

            assertThat(result).isEqualTo(50000L);
        }

        @Test
        void DB에_값이_없으면_기본값을_반환한다() {
            policyRepository.deleteById("MAX_GRANT_PER_TRANSACTION");

            long result = policyService.getMaxGrantPerTransaction();

            assertThat(result).isEqualTo(PointPolicyService.DEFAULT_MAX_GRANT_PER_TRANSACTION);
        }

        @Test
        void DB_값이_숫자가_아니면_기본값을_반환한다() {
            policyRepository.save(new PointPolicy("MAX_GRANT_PER_TRANSACTION", "abc"));

            long result = policyService.getMaxGrantPerTransaction();

            assertThat(result).isEqualTo(PointPolicyService.DEFAULT_MAX_GRANT_PER_TRANSACTION);
        }
    }

    @Nested
    class getMaxBalancePerUser_테스트 {

        @Test
        void DB에_값이_있으면_해당_값을_반환한다() {
            policyRepository.save(new PointPolicy("MAX_BALANCE_PER_USER", "3000000"));

            long result = policyService.getMaxBalancePerUser();

            assertThat(result).isEqualTo(3000000L);
        }

        @Test
        void DB에_값이_없으면_기본값을_반환한다() {
            policyRepository.deleteById("MAX_BALANCE_PER_USER");

            long result = policyService.getMaxBalancePerUser();

            assertThat(result).isEqualTo(PointPolicyService.DEFAULT_MAX_BALANCE_PER_USER);
        }

        @Test
        void DB_값이_숫자가_아니면_기본값을_반환한다() {
            policyRepository.save(new PointPolicy("MAX_BALANCE_PER_USER", "abc"));

            long result = policyService.getMaxBalancePerUser();

            assertThat(result).isEqualTo(PointPolicyService.DEFAULT_MAX_BALANCE_PER_USER);
        }
    }

    @Nested
    class getDefaultExpireDays_테스트 {

        @Test
        void DB에_값이_있으면_해당_값을_반환한다() {
            policyRepository.save(new PointPolicy("DEFAULT_EXPIRE_DAYS", "180"));

            long result = policyService.getDefaultExpireDays();

            assertThat(result).isEqualTo(180L);
        }

        @Test
        void DB에_값이_없으면_기본값을_반환한다() {
            policyRepository.deleteById("DEFAULT_EXPIRE_DAYS");

            long result = policyService.getDefaultExpireDays();

            assertThat(result).isEqualTo(PointPolicyService.DEFAULT_EXPIRE_DAYS);
        }

        @Test
        void DB_값이_숫자가_아니면_기본값을_반환한다() {
            policyRepository.save(new PointPolicy("DEFAULT_EXPIRE_DAYS", "abc"));

            long result = policyService.getDefaultExpireDays();

            assertThat(result).isEqualTo(PointPolicyService.DEFAULT_EXPIRE_DAYS);
        }
    }
}