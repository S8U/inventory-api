package com.github.s8u.inventory.exception;

import lombok.Getter;

/**
 * 비즈니스 예외
 *
 * 응답에는 에러 코드의 고정 메시지가 나간다.
 * 추적에 필요한 값(SKU, 수량 등)은 logMessage로 받아 로그에만 남긴다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String logMessage) {
        super(logMessage);
        this.errorCode = errorCode;
    }

}
