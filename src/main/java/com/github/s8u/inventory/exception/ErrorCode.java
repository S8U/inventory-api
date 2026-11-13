package com.github.s8u.inventory.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드
 *
 * 에러 응답의 code 필드, HTTP 상태, 메시지를 정한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    PRODUCT_STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "재고가 부족합니다."),
    PRODUCT_STOCK_OVERFLOW(HttpStatus.CONFLICT, "재고가 저장할 수 있는 최대값을 넘습니다.");

    private final HttpStatus status;
    private final String message;

}
