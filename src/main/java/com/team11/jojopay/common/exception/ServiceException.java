package com.team11.jojopay.common.exception;

import lombok.Getter;

/**
 * 조조페이(JojoPay) 플랫폼 비즈니스 로직 수행 중 발생하는 예외를 처리하는 전역 커스텀 예외 클래스입니다.
 *
 * 일반적인 RuntimeException을 상속받아 트랜잭션 롤백(@Transactional)을 정상 지원하며,
 * 내부에 발생 원인이 된 ErrorCode를 품고 전역 예외 핸들러(GlobalExceptionHandler)로 날아갑니다.
 */
@Getter
public class ServiceException extends RuntimeException{

    /**
     * 발생한 비즈니스 에러 정의 열거형
     */
    private final ErrorCode errorCode;

    /**
     * 에러와 함께 클라이언트나 로그에 바인딩할 추가 상세 데이터 (없을 경우 null)
     */
    private final Object errorData;

    /**
     * 부가적인 상세 데이터 없이, 에러 코드와 기본 메시지만으로 예외를 생성합니다. (1번 흐름용)
     *
     * @param errorCode 비즈니스 에러 코드 ErrorCode
     */
    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // 부모 RuntimeException에 기본 에러 메시지 주입
        this.errorCode = errorCode;
        this.errorData = null;
    }

    /**
     * 에러 코드와 함께, 에러의 구체적인 원인이 되는 상세 데이터를 포함하여 예외를 생성합니다. (2번 흐름용)
     *
     * @param errorCode 비즈니스 에러 코드 ErrorCode
     * @param errorData 예외 상황과 관련된 상세 데이터 (예: 입력 검증 실패 리스트, 실패 유저 ID 등)
     */
    public ServiceException(ErrorCode errorCode, Object errorData) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errorData = errorData;
    }

}
