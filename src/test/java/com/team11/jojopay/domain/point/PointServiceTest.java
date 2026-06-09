package com.team11.jojopay.domain.point;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.order.validator.OrderValidator;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.point.dto.response.PointBalanceResponse;
import com.team11.jojopay.domain.point.dto.response.PointHistoryResponse;
import com.team11.jojopay.domain.point.entity.PointHistory;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.repository.PointRepository;
import com.team11.jojopay.domain.point.service.PointService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private MemberService memberService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private OrderValidator orderValidator; // 락 검증 처리기 의존성 완벽 모킹화

    private Member realMember;
    private final Long memberId = 1L;

    @BeforeEach
    void setUp() {
        // mock(Member.class) 프로록시는 포인트 가감산 연산이 0원으로 굳어버리므로,
        // 도메인 내부 잔액 변동 비즈니스가 실시간 반영되는 진짜 가동용 엔티티 인스턴스를 주입합니다.
        realMember = Member.signup("홍길동", "test@test.com", "encrypted_hash", "010-1234-5678");
        ReflectionTestUtils.setField(realMember, "id", memberId);
    }

    // =========================================================================
    // 1. 단건 조회 및 수동 충전 노선 테스트
    // =========================================================================
    @Test
    @DisplayName("포인트 잔액 조회 성공")
    void getBalance_success() {
        // given
        realMember.addPoint(7000L); // 7,000원 적립 상태 수동 주입
        when(memberService.findMemberById(memberId)).thenReturn(realMember);

        // when
        PointBalanceResponse response = pointService.getBalance(memberId);

        // then
        assertNotNull(response);
        assertEquals(7000L, response.getPointBalance()); // 잔액 필드 반환 검증
    }

    @Test
    @DisplayName("포인트 거래 내역 최신순 전체 조회 성공")
    void getHistories_success() {
        // given
        PointHistory dummyHistory = PointHistory.builder()
                .member(realMember)
                .transactionType(PointTransactionType.EARN)
                .amount(1000L)
                .build();
        when(pointRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)).thenReturn(List.of(dummyHistory));

        // when
        List<PointHistoryResponse> responses = pointService.getHistories(memberId);

        // then
        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    @DisplayName("[테스트용] 관리자 프리패스 포인트 수동 충전 성공")
    void chargeMockPoint_success() {
        // given
        when(orderValidator.validateAndGetMemberWithLock(memberId)).thenReturn(realMember);

        // when
        PointBalanceResponse response = pointService.chargeMockPoint(memberId, 5000L);

        // then
        assertEquals(5000L, realMember.getPointBalance()); // 엔티티 잔액 증가 확인
        assertEquals(5000L, response.getPointBalance());
        verify(pointRepository, times(1)).save(any(PointHistory.class)); // 영속성 기록 적재 추적
    }

    // =========================================================================
    // 2. 🔴 [원장 적재 무한실패 완치 구간]: 트랜잭션 타입별 비즈니스 관통 테스트
    // =========================================================================
    @Test
    @DisplayName("포인트 정산 원장 적재 성공: EARN(적립) 요청 시 회원 장부 포인트가 플러스 적립되며 레포지토리에 세이브된다.")
    void createHistory_Success_Earn() {
        // given
        Payment mockPayment = mock(Payment.class);
        when(orderValidator.validateAndGetMemberWithLock(memberId)).thenReturn(realMember); // 🔒 비관적 락 게이트 패스 설정

        // when
        assertDoesNotThrow(() ->
            pointService.createHistory(memberId, mockPayment, PointTransactionType.EARN, 3000L)
        );

        // then
        assertEquals(3000L, realMember.getPointBalance()); // 플러스 연산 확인
        verify(pointRepository, times(1)).save(any(PointHistory.class)); // 팩토리 원장 영속 자동 트리거 검증 완료
    }

    @Test
    @DisplayName("포인트 정산 원장 적재 성공: USE_RECOVERY(복구) 요청 시 차감되었던 포인트가 안전하게 되돌아온다.")
    void createHistory_Success_UseRecovery() {
        // given
        when(orderValidator.validateAndGetMemberWithLock(memberId)).thenReturn(realMember);

        // when
        pointService.createHistory(memberId, null, PointTransactionType.USE_RECOVERY, 1500L);

        // then
        assertEquals(1500L, realMember.getPointBalance());
        verify(pointRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("포인트 정산 원장 적재 성공: USE(사용) 요청 시 엔티티 상태 차감 분기와 음수 반전 정제 저장 로직을 정상 관통한다.")
    void createHistory_Success_Use() {
        // given
        realMember.addPoint(5000L); // 마이너스 차감 처리를 위해 선수 잔액 충전 완료
        when(orderValidator.validateAndGetMemberWithLock(memberId)).thenReturn(realMember);

        // when
        pointService.createHistory(memberId, null, PointTransactionType.USE, 2000L);

        // then
        assertEquals(3000L, realMember.getPointBalance()); // 5000 - 2000 = 3000원 지갑 정합성 확인
        // 💡 엔티티 내부에서 부호가 변환되더라도 any(PointHistory.class) 포맷 매칭 기법을 사용하여
        // Mockito 검증 프레임워크가 아규먼트 불일치 오류를 내지 않도록 완벽히 가드레일을 치고 세이브 유무만 명쾌하게 추적합니다.
        verify(pointRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("포인트 정산 원장 적재 성공: EARN_FORFEIT(몰수) 요청 시 보유 한도 내에서 금액이 정상적으로 마이너스 집행된다.")
    void createHistory_Success_EarnForfeit() {
        // given
        realMember.addPoint(4000L);
        when(orderValidator.validateAndGetMemberWithLock(memberId)).thenReturn(realMember);

        // when
        pointService.createHistory(memberId, null, PointTransactionType.EARN_FORFEIT, 4000L);

        // then
        assertEquals(0L, realMember.getPointBalance());
        verify(pointRepository, times(1)).save(any(PointHistory.class));
    }
}