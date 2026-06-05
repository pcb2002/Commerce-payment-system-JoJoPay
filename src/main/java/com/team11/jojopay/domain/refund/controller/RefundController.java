package com.team11.jojopay.domain.refund.controller;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.refund.dto.request.RefundRequest;
import com.team11.jojopay.domain.refund.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class RefundController {

    private final RefundService refundService;

    /**
     * 8. 주문 상품 부분 및 전액 환불 API
     * POST /api/v1/orders/{orderId}/refund
     */
    @PostMapping("/{orderId}/refund")
    public CommonApiResponse<Void> refundOrder(@AuthenticationPrincipal Long memberId, @PathVariable Long orderId, @Valid @RequestBody RefundRequest request) {
        refundService.refundOrder(memberId, request);
        return CommonApiResponse.success(HttpStatus.OK, "환불 처리가 완료되었습니다.", null);
    }
}
