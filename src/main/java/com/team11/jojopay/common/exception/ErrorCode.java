package com.team11.jojopay.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 조조페이(JojoPay) 플랫폼 전역에서 발생하는 비즈니스 에러 코드를 관리하는 Enum 클래스입니다.
 * 각 에러 코드는 추적 편의성을 위해 HTTP 상태 코드(HttpStatus), 고유 에러 식별 문자열(code), 클라이언트 반환용 메시지(message)를 함께 묶어 정의합니다.
 *
 */
@Getter
public enum ErrorCode {

    // =========================================================================
    // 400 BAD_REQUEST: 잘못된 요청 규격 및 검증 실패
    // =========================================================================
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값이 올바르지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRM_MISMATCH", "비밀번호 확인이 일치하지 않습니다."),
    PASSWORD_NEW_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_NEW_CONFIRM_MISMATCH", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "주문 또는 결제 수량이 올바르지 않습니다."),
    INVALID_STOCK_VALUE(HttpStatus.BAD_REQUEST, "INVALID_STOCK_VALUE", "재고값이 올바르지 않습니다."),
    INVALID_AMOUNT_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT_FORMAT", "충전 또는 결제 금액 단위를 다시 확인해주세요."),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_POINT_AMOUNT", "사용 포인트가 결제 금액을 초과할 수 없습니다."),

    // 상태값(Status) 도메인 검증
    INVALID_MEMBER_STATUS(HttpStatus.BAD_REQUEST, "INVALID_MEMBER_STATUS", "유효하지 않은 회원 상태입니다."),
    INVALID_PRODUCT_STATUS(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_STATUS", "유효하지 않은 상품 상태입니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "INVALID_ORDER_STATUS", "유효하지 않은 주문 상태입니다."),


    // =========================================================================
    // 401 UNAUTHORIZED: 인증 자격 증명 실패 및 JWT 예외 처리
    // =========================================================================
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "TOKEN_REQUIRED", "인증 토큰이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "인증 토큰이 만료되었습니다."),
    INVALID_TOKEN_SIGNATURE(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_SIGNATURE", "토큰 서명이 유효하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    INVALID_TOKEN_FORMAT(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_FORMAT", "토큰 형식이 올바르지 않습니다."),


    // =========================================================================
    // 403 FORBIDDEN: 인가 실패 및 계정 정지 권한 제한
    // =========================================================================
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "MEMBER_SUSPENDED", "활동이 정지된 회원 계정입니다."),
    INVALID_PASSWORD(HttpStatus.FORBIDDEN, "INVALID_PASSWORD", "현재 비밀번호가 올바르지 않습니다."),


    // =========================================================================
    // 404 NOT_FOUND: 리소스 존재하지 않음
    // =========================================================================
    INVALID_CREDENTIALS(HttpStatus.NOT_FOUND, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원이 존재하지 않습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품이 존재하지 않습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문 내역이 존재하지 않습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 내역을 찾을 수 없습니다."),
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "회원의 페이머니 지갑을 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "장바구니 항목을 찾을 수 없습니다."),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."),


    // =========================================================================
    // 409 CONFLICT: 비즈니스 정합성 충돌 및 도메인 정책 위반
    // =========================================================================
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "EMAIL_DUPLICATE", "이미 사용 중인 이메일입니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "상품 재고가 부족합니다."),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.CONFLICT, "ORDER_CANNOT_BE_CANCELLED", "이미 결제가 완료된 주문은 취소할 수 없습니다."),
    ORDER_ALREADY_BE_CANCELLED(HttpStatus.CONFLICT, "ORDER_ALREADY_BE_CANCELLED", "이미 취소 처리된 주문입니다."),
    PERIODIC_PAYMENT_FAILED(HttpStatus.CONFLICT, "PERIODIC_PAYMENT_FAILED", "정기 구독 결제 승인 요청에 실패했습니다."),
    PRODUCT_DISCONTINUED(HttpStatus.CONFLICT, "PRODUCT_DISCONTINUED", "판매가 중단(단종)된 전자제품은 주문할 수 없습니다."),
    PRODUCT_ALREADY_DELETED(HttpStatus.CONFLICT, "PRODUCT_ALREADY_DELETED", "이미 삭제 처리된 상품입니다."),
    MEMBER_ALREADY_DELETED(HttpStatus.CONFLICT, "MEMBER_ALREADY_DELETED", "이미 탈퇴 또는 삭제 처리된 회원입니다."),
    ALREADY_ACTIVE_SUBSCRIPTION(HttpStatus.CONFLICT, "ALREADY_ACTIVE_SUBSCRIPTION", "이미 활성 구독이 존재합니다."),
    NO_ACTIVE_SUBSCRIPTION(HttpStatus.CONFLICT, "NO_ACTIVE_SUBSCRIPTION", "해지할 활성 구독이 존재하지 않습니다."),

    // 조조페이머니 핵심 비즈니스 정합성 정책
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", "페이머니 잔액이 부족하여 결제할 수 없습니다."),
    PAYMENT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "PAYMENT_LIMIT_EXCEEDED", "1회 혹은 1일 충전 한도를 초과했습니다."),
    ORDER_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "ORDER_CANCEL_NOT_ALLOWED", "이미 배송이 시작되었거나 취소할 수 없는 상태의 주문입니다."),
    PERIODIC_PAYMENT_FAILED(HttpStatus.CONFLICT, "PERIODIC_PAYMENT_FAILED", "정기 구독 결제 승인 요청에 실패했습니다."),

    // =========================================================================
    // 500 INTERNAL_SERVER_ERROR: 서버 치명적 오류
    // =========================================================================
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    PAYMENT_GATEWAY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_GATEWAY_ERROR", "결제 대행사(PG) 연동 중 통신 오류가 발생했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_CANCEL_FAILED", "금액 불일치로 인한 외부 PG 결제 보상 취소 요청이 실패했습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    /**
     * 프론트엔드 파싱 및 내부 규격화를 위해 HttpStatus의 정수형 상태값(value)을 변환하여 반환합니다.
     *
     * @return HTTP 상태 코드 숫자 (예: 200, 400, 404)
     */
    public int getStatus() {
        return httpStatus.value();
    }
}
