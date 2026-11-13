package com.github.s8u.inventory.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드
 *
 * 에러 응답의 code 필드, HTTP 상태, 메시지를 정한다.
 * INVALID_REQUEST만 예외로, 상태와 메시지를 스프링이 정한 값으로 응답한다 (없는 경로 404, 검증 실패 메시지 등).
 * description은 API 문서(Swagger)의 에러 응답 설명에 쓰는 짧은 이름이다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청", "요청 값이 잘못되었습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류", "서버 오류가 발생했습니다."),

    // 상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품 없음", "상품을 찾을 수 없습니다."),
    PRODUCT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "상품명 누락", "처음 입고하는 상품은 상품명이 필요합니다."),
    PRODUCT_STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "상품 재고 부족", "재고가 부족합니다."),
    PRODUCT_STOCK_OVERFLOW(HttpStatus.CONFLICT, "상품 재고 최대값 초과", "재고가 저장할 수 있는 최대값을 넘습니다.");

    private final HttpStatus status;
    private final String description;
    private final String message;

}
