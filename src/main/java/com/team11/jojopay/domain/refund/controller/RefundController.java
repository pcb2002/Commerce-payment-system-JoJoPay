package com.team11.jojopay.domain.refund.controller;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.refund.dto.request.RefundRequest;
import com.team11.jojopay.domain.refund.dto.response.RefundResponse;
import com.team11.jojopay.domain.refund.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 환불(Refund) 도메인의 비즈니스 API를 외부에 노출하는 컨트롤러 클래스입니다.
 * 클라이언트의 환불 요청 규격을 검증하고 환불 서비스로 처리를 위임합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class RefundController {

    private final RefundService refundService;

    /**
     * 주문 상품에 대한 부분 환불 또는 전액 환불을 수행합니다.
     * 가독성을 위해 URL 경로는 직관적인 주문 식별자({orderId})를 활용하지만,
     * 실제 비즈니스 로직 및 교차 검증은 DTO에 포함된 고유 비즈니스 주문 번호(orderNumber)를 기반으로 처리됩니다.
     *
     * @param memberId     인증 필터를 통해 시큐리티 컨텍스트에서 추출한 로그인 회원의 고유 식별자 ID
     * @param orderId      URL 경로로 전달된 대상 주문의 데이터베이스 PK (자원 식별용)
     * @param request      환불 사유 및 상세 주문 상품 항목별 수량 정보가 포함된 요청 DTO
     * @return 환불 성공 메시지와 HTTP 200 OK 상태를 담은 CommonApiResponse 객체
     */
    @PostMapping("/{orderId}/refund")
    public CommonApiResponse<Void> refundOrder(@AuthenticationPrincipal Long memberId, @PathVariable Long orderId, @Valid @RequestBody RefundRequest request) {
        refundService.refundOrder(memberId, request);
        return CommonApiResponse.success(HttpStatus.OK, "환불 처리가 완료되었습니다.", null);
    }

    @GetMapping("/my")
    public CommonApiResponse<List<RefundResponse>> getMyRefunds(
            @AuthenticationPrincipal Long memberId) {
        return CommonApiResponse.success(HttpStatus.OK, "내 환불 목록", refundService.getMyRefunds(memberId));
    }
}
