package com.pointsystem.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "입력값이 유효하지 않습니다."),

    //적립
    GRANT_AMOUNT_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "GRANT_001", "적립 금액은 1원 이상, 정책 상한 이하여야 합니다."),
    GRANT_EXPIRES_AT_INVALID(HttpStatus.BAD_REQUEST, "GRANT_002", "만료일은 1일 이상, 5년 미만이어야 합니다."),
    GRANT_BALANCE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "GRANT_003", "고객 보유 한도를 초과합니다."),
    GRANT_NOT_FOUND(HttpStatus.NOT_FOUND, "GRANT_004", "적립 내역을 찾을 수 없습니다."),
    GRANT_ALREADY_USED(HttpStatus.CONFLICT, "GRANT_005", "이미 사용된 적립은 취소할 수 없습니다."),
    GRANT_ALREADY_CANCELED(HttpStatus.CONFLICT, "GRANT_006", "이미 취소된 적립입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
