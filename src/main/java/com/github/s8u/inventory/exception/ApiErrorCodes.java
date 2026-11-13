package com.github.s8u.inventory.exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API가 응답할 수 있는 에러 코드
 *
 * SwaggerConfig가 이 목록으로 API 문서의 에러 응답(상태별 설명과 예시)을 만든다.
 * 처리하지 못한 예외(INTERNAL_ERROR)는 모든 API에 붙으므로 적지 않는다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {

    ErrorCode[] value();

}
