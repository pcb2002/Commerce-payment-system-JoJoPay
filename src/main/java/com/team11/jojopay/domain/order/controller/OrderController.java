package com.team11.jojopay.domain.order.controller;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.order.dto.request.OrderPreviewRequest;
import com.team11.jojopay.domain.order.dto.response.OrderPreviewResponse;
import com.team11.jojopay.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}