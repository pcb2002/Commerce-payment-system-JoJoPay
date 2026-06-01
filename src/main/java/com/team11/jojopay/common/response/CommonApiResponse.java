package com.team11.jojopay.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.team11.jojopay.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션의 전역 표준 API 응답을 형상화하는 공통 응답 DTO(Data Transfer Object) 클래스입니다.
 * 모든 컨트롤러의 API 반환 타입은 이 클래스로 래핑되어 일관된 응답 포맷(상태 코드, 커스텀 에러 코드, 메시지, 데이터)을 보장합니다.
 *
 * @param <T> 실제 응답 데이터 타입
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonApiResponse<T> {

    private final int status;       // HTTP 상태코드
    private final String code;      // 비즈니스 로직 단위의 커스텀 에러 코드 (성공 시에는 null)
    private final String message;   // 응답 결과에 대한 설명 또는 에러 메시지
    private final T data;           // API가 최종적으로 반환하는 실제 본문 데이터 (데이터가 없는 경우 null)

    /**
     * 규칙에 따른 private 생성자 — 외부에서 new 키워드를 통한 무분별한 인스턴스 생성을 제한합니다.
     */
    private CommonApiResponse(int status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 성공적인 API 요청에 대한 공통 응답 객체를 생성합니다.
     *
     * @param <T>     응답 데이터의 타입
     * @param status  HTTP 상태 열거형
     * @param message 성공 안내 메시지
     * @param data    클라이언트에게 전달할 결과 데이터 객체
     * @return 형식이 통일된 성공 응답 객체 (CommonApiResponse)
     */
    public static <T> CommonApiResponse<T> success(HttpStatus status, String message, T data) {
        return new CommonApiResponse<>(status.value(), null, message, data);
    }

    /**
     * 내부 비즈니스 예외 발생 시, 부가적인 데이터 없이 규격화된 실패 응답 객체를 생성합니다.
     *
     * @param errorCode 전역 에러 정의 열거형
     * @return 본문 데이터가 비어있는(Void) 실패 응답 객체
     */
    public static CommonApiResponse<Void> error(ErrorCode errorCode) {
        return new CommonApiResponse<>(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 비즈니스 예외 발생 시 에러 원인이 된 데이터나 바인딩 에러 목록(Validation Fail) 등을 포함한 실패 응답 객체를 생성합니다.
     *
     * @param <T>       에러 부가 데이터의 타입
     * @param errorCode 전역 에러 정의 열거형
     * @param data      에러 상황과 관련된 상세 데이터 (예: 유효성 검증 실패 필드 목록 등)
     * @return 에러 세부 정보 데이터가 포함된 실패 응답 객체
     */
    public static <T> CommonApiResponse<T> error(ErrorCode errorCode, T data) {
        return new CommonApiResponse<>(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), data);
    }
}
