package com.pointsystem.point.service;

import com.pointsystem.point.domain.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointPolicyService {
    public static final String MAX_GRANT_PER_TRANSACTION_CONFIG = "MAX_GRANT_PER_TRANSACTION"; //거래당 최대 부여 가능 금액
    public static final String MAX_BALANCE_PER_USER_CONFIG = "MAX_BALANCE_PER_USER"; //유저당 보유가능한 최대 잔애
    public static final String EXPIRE_DAYS_CONFIG = "DEFAULT_EXPIRE_DAYS"; // 포인트 유효기간
    public static final long DEFAULT_MAX_GRANT_PER_TRANSACTION = 100_000;
    public static final long DEFAULT_MAX_BALANCE_PER_USER = 5_000_000;
    public static final long DEFAULT_EXPIRE_DAYS = 365;
    private final PointPolicyRepository pointPolicyRepository;

    //TODO: 성능 위해 추후 캐시 적용
    public long getMaxGrantPerTransaction() {
        return getPolicyValue(MAX_GRANT_PER_TRANSACTION_CONFIG, DEFAULT_MAX_GRANT_PER_TRANSACTION);
    }

    //TODO: 성능 위해 추후 캐시 적용
    public long getMaxBalancePerUser() {
        return getPolicyValue(MAX_BALANCE_PER_USER_CONFIG, DEFAULT_MAX_BALANCE_PER_USER);
    }

    //TODO: 성능 위해 추후 캐시 적용
    public long getDefaultExpireDays() {
        return getPolicyValue(EXPIRE_DAYS_CONFIG, DEFAULT_EXPIRE_DAYS);
    }

    private long getPolicyValue(String key, long defaultValue) {
        try {
            long value = pointPolicyRepository.findById(key)
                    .map(it -> Long.parseLong(it.getPolicyValue()))
                    .orElse(defaultValue);
            log.debug("정책 조회 성공: key={}, value={}", key, value);
            return value;
        } catch (Exception e) {
            log.warn("정책 조회 실패, 기본값 사용: key={}, defaultValue={}", key, defaultValue, e);
            return defaultValue;
        }
    }
}
