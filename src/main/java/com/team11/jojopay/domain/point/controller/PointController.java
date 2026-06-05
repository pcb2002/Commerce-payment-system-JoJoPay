package com.team11.jojopay.domain.point.controller;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.point.dto.response.PointBalanceResponse;
import com.team11.jojopay.domain.point.dto.response.PointHistoryResponse;
import com.team11.jojopay.domain.point.service.PointService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /**
     * 본인의 현재 포인트 잔액을 조회합니다.
     * SecurityConfig에서 전역 인증을 강제하므로 로그인된 회원만 접근 가능합니다.
     *
     * @param memberId 인증 토큰(JWT)에서 추출된 회원의 고유 식별자 ID
     * @return 공통 응답 규격에 감싸진 현재 포인트 잔액 정보 (PointBalanceResponse)
     */
    @GetMapping("/balance")
    public CommonApiResponse<PointBalanceResponse> getBalance(@AuthenticationPrincipal Long memberId) {
        PointBalanceResponse response = pointService.getBalance(memberId);
        return CommonApiResponse.success(HttpStatus.OK, "포인트 잔액 조회 성공", response);
    }

    /**
     * 본인의 포인트 사용·적립·취소 이력 전체를 최신순(생성일 내림차순)으로 조회합니다.
     *
     * @param memberId 인증 토큰(JWT)에서 추출된 회원의 고유 식별자 ID
     * @return 최신순으로 정렬된 포인트 거래 내역 목록 (List<PointHistoryResponse>)
     */
    @GetMapping("/histories")
    public CommonApiResponse<List<PointHistoryResponse>> getHistories(@AuthenticationPrincipal Long memberId) {
        List<PointHistoryResponse> response = pointService.getHistories(memberId);
        return CommonApiResponse.success(HttpStatus.OK, "포인트 거래 내역 전체 최신순으로 조회 성공", response);
    }

    /**
     * [테스트 전용 더미 기능] 관리자 기능 범위 외의 테스트를 위해 더미 데이터로 포인트를 충전합니다.
     * 최소 1원 이상부터 충전이 가능하도록 유효성 검증을 포함합니다.
     *
     * @param memberId 인증 토큰(JWT)에서 추출된 회원의 고유 식별자 ID
     * @param amount   충전하고자 하는 포인트 금액 (최소 1원 이상)
     * @return 충전 처리가 완료된 후 갱신된 회원의 포인트 잔액 정보 (PointBalanceResponse)
     */
    @PostMapping("/mock-charge")
    public CommonApiResponse<PointBalanceResponse> chargeMockPoint(@AuthenticationPrincipal Long memberId, @RequestParam("amount") @Min(value = 1, message = "최소 1원 이상부터 충전할 수 있습니다.") Long amount) {
        PointBalanceResponse response = pointService.chargeMockPoint(memberId, amount);
        return CommonApiResponse.success(HttpStatus.CREATED, "더미 포인트 충전 성공", response);
    }
}
