package com.team11.jojopay.common.exception;

import com.team11.jojopay.common.response.CommonApiResponse;
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
     * 서비스 레이어에서 비즈니스 로직 위반으로 던진 커스텀 예외(ServiceException)를 처리합니다.
     * ErrorCode가 401이든, 404든, 409든 상관없이 동적으로 판단하여 클라이언트에게 내려줍니다.
     *
     * @param e 발생한 ServiceException 객체
     * @return  ResponseEn
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<CommonApiResponse<Object>> handleServiceException(ServiceException e) {

        // 1. 서비스가 던진 폭탄(예외) 속에 들어있는 '진짜 에러코드'를 쏙 빼옵니다.
        // 예: ErrorCode.CUSTOMER_NOT_FOUND (404) 또는 EMAIL_DUPLICATE (409) 등
        ErrorCode errorCode = e.getErrorCode();

        // 2. 공통 응답 상자에 에러 코드를 넣어서 포장합니다. (data는 null)
        CommonApiResponse<Object> response = CommonApiResponse.error(errorCode, e.getErrorData());

        // 3. 고정된 HttpStatus가 아니라, 에러코드가 품고 있는 진짜 HTTP 상태값을 동적으로 꺼내서 매핑합니다!
        return ResponseEntity
                .status(errorCode.getHttpStatus()) // ➔ 여기서 401, 404, 409 등이 알아서 결정됩니다!
                .body(response);
    }

    /**
     * 자바/스프링 표준 예외인 IllegalArgumentException은 에러코드가 내장되어 있지 않으므로,
     * 개발자가 지정한 표준 에러코드(VALIDATION_FAILED - 400)로 묶어서 처리합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonApiResponse<String>> handleIllegalArgument(IllegalArgumentException e) {
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        CommonApiResponse<String> response = CommonApiResponse.error(errorCode, e.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus()) // 하드코딩 대신 열거형에서 꺼내 쓰도록 수정!
                .body(response);
    }

}
