package com.team11.jojopay.domain.order.controller;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.order.dto.request.OrderCreateRequest;
import com.team11.jojopay.domain.order.dto.request.OrderPreviewRequest;
import com.team11.jojopay.domain.order.dto.response.*;
import com.team11.jojopay.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.OK;

/**
 * 주문 도메인의 API 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문서 미리보기 API를 처리합니다.
     * 아직 주문 레코드를 생성하거나 상품 스냅샷을 저장하기 전 단계로, 데이터베이스 갱신(저장) 로직은 포함되지 않습니다.
     *
     * @param memberId JWT 토큰에서 추출한 인증된 회원 식별자
     * @param request 조회할 장바구니 아이템 ID 목록 (선택 사항)
     * @return 200 OK 상태 코드와 결제 화면 구성에 필요한 주문 예상 정보(실시간 가격, 총액 등)
     */
    @PostMapping("/preview")
    public CommonApiResponse<OrderPreviewResponse> preview(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody OrderPreviewRequest request) {

        // 서비스 호출 및 응답 반환
        OrderPreviewResponse response = orderService.preview(memberId, request);
        return CommonApiResponse.success(OK, "주문서 미리보기 성공", response);
    }

    /**
     * [주문 생성]
     * 비즈니스 로직 없이 Service 호출 후 CommonApiResponse로 감싸서 반환합니다.
     */
    @PostMapping
    public CommonApiResponse<OrderResponse> createOrder(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody OrderCreateRequest request) {

        OrderResponse response = orderService.createOrder(memberId, request);
        return CommonApiResponse.success(OK, "주문 성공", response); // 규약에 맞춘 공통 응답 객체
    }

    /**
     * [주문 내역 목록 조회]
     * 회원의 전체 주문 내역을 페이징하여 최신순으로 제공합니다.
     */
    @GetMapping
    public CommonApiResponse<Page<OrderListItemResponse>> getMyOrders(
            @AuthenticationPrincipal Long memberId,
            Pageable pageable) { // ?page=0&size=10 형식의 쿼리 파라미터를 자동 바인딩합니다.

        Page<OrderListItemResponse> response = orderService.getMyOrders(memberId, pageable);
        return CommonApiResponse.success(OK, "주문 내역 목록 조회 성공", response);
    }

    /**
     * [단건 주문 상세 조회]
     * 특정 주문의 상세 정보, 상품 스냅샷, 결제/포인트 내역을 제공합니다.
     */
    @GetMapping("/{orderNumber}")
    public CommonApiResponse<OrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable String orderNumber) {

        OrderDetailResponse response = orderService.getOrderDetail(memberId, orderNumber);
        return CommonApiResponse.success(OK, "단건 주문 상세 조회 성공", response);
    }

    /**
     * [주문 취소]
     * 결제 대기 상태인 주문을 취소하고 선차감된 재고를 즉시 복구합니다.
     */
    @PostMapping("/{orderNumber}/cancel")
    public CommonApiResponse<OrderCancelResponse> cancelOrder(
            @AuthenticationPrincipal Long memberId,
            @PathVariable String orderNumber) {

        OrderCancelResponse response = orderService.cancelOrder(memberId, orderNumber);

        return CommonApiResponse.success(OK, "주문 취소 성공", response);
    }
}