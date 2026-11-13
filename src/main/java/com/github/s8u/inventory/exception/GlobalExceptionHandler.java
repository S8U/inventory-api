package com.github.s8u.inventory.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리
 *
 * 모든 에러를 RFC 9457 Problem Details 형식으로 응답하고, 에러 종류를 나타내는 code 필드를 붙인다.
 * 스프링 MVC가 컨트롤러 전에 거른 요청(검증 실패, JSON 형식 오류, 없는 경로 등)은 ResponseEntityExceptionHandler가 처리한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        log.warn("Business exception code={}, {}", ex.getErrorCode(), ex.getMessage());
        return toResponse(ex.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception ex) {
        log.error("Unexpected exception", ex);
        // 내부 예외 메시지는 응답에 담지 않는다
        return toResponse(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * 요청 본문 검증 실패
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ex.getBody().setDetail(joinMessages(ex.getBindingResult().getAllErrors()));
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    /**
     * 경로 변수 검증 실패 (경로 변수와 요청 본문이 함께 틀리면 둘 다 담긴다)
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ex.getBody().setDetail(joinMessages(ex.getAllErrors()));
        return super.handleHandlerMethodValidationException(ex, headers, status, request);
    }

    /**
     * 요청 본문을 읽지 못함 (JSON 형식 오류, 수량에 숫자가 아닌 값 등)
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail body = createProblemDetail(
                ex, status, "요청 본문을 읽을 수 없습니다. JSON 형식과 값의 타입을 확인해 주세요.", null, null, request
        );
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * 스프링 MVC가 처리한 예외의 응답에도 code를 붙인다
     *
     * 상태는 스프링이 정한 값(대부분 400, 없는 경로 404, 허용하지 않는 메서드 405 등)을 그대로 쓴다.
     * Content-Type은 toResponse와 같은 이유로 application/problem+json으로 정해 둔다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (statusCode.is5xxServerError())
            log.error("Spring MVC exception", ex);

        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response == null || !(response.getBody() instanceof ProblemDetail problemDetail))
            return response;

        ErrorCode errorCode = statusCode.is4xxClientError() ? ErrorCode.INVALID_REQUEST : ErrorCode.INTERNAL_ERROR;
        problemDetail.setProperty("code", errorCode.name());

        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    // Content-Type을 정해 두지 않으면, Accept 헤더가 잘못된 요청에서 스프링이 에러 본문을 쓰지 않고 빈 응답을 보낸다
    private ResponseEntity<ProblemDetail> toResponse(ErrorCode errorCode) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
        problemDetail.setProperty("code", errorCode.name());

        return ResponseEntity.status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    // 여러 검증 오류를 한 문장으로 합친다. 순서가 매번 같도록 정렬한다
    private String joinMessages(List<? extends MessageSourceResolvable> errors) {
        return errors.stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining(", "));
    }

}
