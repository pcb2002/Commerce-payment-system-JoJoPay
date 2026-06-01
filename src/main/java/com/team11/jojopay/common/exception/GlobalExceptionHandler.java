package com.team11.jojopay.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 애플리케이션 전역에서 발생하는 예외(Exception)를 감지하고 일괄적으로 처리하는 중앙 집중식 예외 핸들러 클래스입니다.
 * 모든 Controller에서 던져지는 예외를 가로채어 클라이언트에게 일관된 형식의 HTTP 에러 응답을 반환하는 역할을 수행합니다.
 *
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 잘못된 인자나 인수가 메서드에 전달되었을 때 발생하는 IllegalArgumentException을 전역적으로 처리합니다.
     * 주로 비즈니스 로직 검증 실패, 필수 값 누락 등의 상황에서 발생하며,
     * 클라이언트에게는 HTTP 400 Bad Request 상태 코드와 함께 해당 예외의 메시지를 본문(Body)으로 반환합니다.
     *
     * @param e 발생한 IllegalArgumentException 객체
     * @return 에러 메시지와 HttpStatus#BAD_REQUEST 상태 코드를 포함한 ResponseEntity
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

}
