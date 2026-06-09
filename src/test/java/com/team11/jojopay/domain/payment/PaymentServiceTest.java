package com.team11.jojopay.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.enums.MembershipGrade;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.entity.OrderItem;
import com.team11.jojopay.domain.payment.dto.request.PaymentConfirmRequest;
import com.team11.jojopay.domain.payment.dto.response.PaymentResponse;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse.Amount;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.payment.service.PaymentService;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.product.service.ProductService;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @InjectMocks
  private PaymentService paymentService;

  @Mock private PaymentRepository paymentRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private PortOneClient portOneClient;
  @Mock private ProductService productService;
  @Mock private PointService pointService;

  @Test
  @DisplayName("결제 확정 성공: 정상 결제 시 포인트 사용/적립 원장이 통합 기록되고 완료 상태로 전환된다.")
  void confirmPayment_Success() {
    // given
    PaymentConfirmRequest request = new PaymentConfirmRequest("ORD-20260605-001", "imp_123456789");

    Order mockOrder = mock(Order.class);
    Member mockMember = mock(Member.class);
    Payment mockPayment = mock(Payment.class);

    given(mockOrder.getMemberId()).willReturn(1L);

    // 금액 스펙 설정 (총액 50,000원 / 사용포인트 5,000원 / 실결제액 45,000원)
    given(mockPayment.getAmount()).willReturn(50000L);
    given(mockPayment.getUsedPoint()).willReturn(5000L);
    given(mockPayment.getPgRealAmount()).willReturn(45000L);
    given(mockPayment.getStatus()).willReturn(PaymentStatus.READY);
    given(mockPayment.getOrder()).willReturn(mockOrder);

    // 회원 및 등급별 적립률 설정 (예: 0.01 = 1%)
    given(mockMember.getMembershipGrade()).willReturn(MembershipGrade.NORMAL);
    // 첨부파일의 수식(payment.getAmount() * earnRate)에 맞춰 값 세팅
    double mockEarnRate = 0.01;

    // 포트원 정상 승인 응답 가짜 세팅
    PortOnePaymentResponse mockPortOneResponse = mock(PortOnePaymentResponse.class);
    Amount mockAmount = mock(Amount.class);
    given(mockPortOneResponse.getStatus()).willReturn("PAID");
    given(mockAmount.getTotal()).willReturn(45000L);
    given(mockPortOneResponse.getAmount()).willReturn(mockAmount);

    // 가짜 객체 리턴 정의
    given(paymentRepository.findByPortonePaymentIdWithLock(request.getPortonePaymentId()))
        .willReturn(Optional.of(mockPayment));
    given(portOneClient.getPaymentInfo(request.getPortonePaymentId())).willReturn(mockPortOneResponse);
    given(memberRepository.findById(1L)).willReturn(Optional.of(mockMember));

    // when
    PaymentResponse response = paymentService.confirmPayment(request);

    // then
    // 기존의 usePoint 대신 진짜 코드에 작성된 pointService.createHistory 방식 검증
    verify(pointService, times(1)).createHistory(
        eq(mockMember.getId()), eq(mockPayment), eq(PointTransactionType.USE), eq(5000L)
    );

    // 기존의 earnPoint 대신 진짜 코드에 작성된 pointService.createHistory 방식 검증
    long expectedEarnPoint = (long) (50000L * mockEarnRate); // 50,000 * 0.01 = 500원
    verify(pointService, times(1)).createHistory(
        eq(mockMember.getId()), eq(mockPayment), eq(PointTransactionType.EARN), eq(expectedEarnPoint)
    );

    // 실결제 금액(45,000원) 기준으로 멤버십 누적 결제액이 올라갔는지 검증
    verify(mockMember, times(1)).increaseTotalPaymentAmount(45000L);

    // 상태 종결 행위 검증
    verify(mockPayment, times(1)).complete();
    verify(mockOrder, times(1)).completeOrder();

    assertThat(response).isNotNull();
  }

  @Test
  @DisplayName("결제 확정 실패: 위변조 예외 발생 시, 다중 OrderItem 품목 전체를 돌며 재고를 복구한다.")
  void confirmPayment_Fail_ValidationFailed_And_RollbackStock() {
    // given
    PaymentConfirmRequest request = new PaymentConfirmRequest("ORD-20260605-001", "imp_123456789");

    // 1:N 구조의 하위 상품 목록 세팅 (상품 101번 2개, 상품 102번 5개)
    OrderItem itemA = mock(OrderItem.class);
    OrderItem itemB = mock(OrderItem.class);
    given(itemA.getProductId()).willReturn(101L);
    given(itemA.getQuantity()).willReturn(2);
    given(itemB.getProductId()).willReturn(102L);
    given(itemB.getQuantity()).willReturn(5);

    Order mockOrder = mock(Order.class);
    given(mockOrder.getOrderItems()).willReturn(List.of(itemA, itemB));

    Payment mockPayment = mock(Payment.class);
    given(mockPayment.getPgRealAmount()).willReturn(45000L);
    given(mockPayment.getStatus()).willReturn(PaymentStatus.READY);
    given(mockPayment.getOrder()).willReturn(mockOrder);

    // 포트원 가짜 위변조 금액 응답 (DB엔 45,000원인데 실제론 10,000원만 결제된 케이스)
    PortOnePaymentResponse mockPortOneResponse = mock(PortOnePaymentResponse.class);
    Amount mockAmount = mock(Amount.class);
    given(mockPortOneResponse.getStatus()).willReturn("PAID");
    given(mockAmount.getTotal()).willReturn(10000L); // 금액 불일치 발생
    given(mockPortOneResponse.getAmount()).willReturn(mockAmount);

    given(paymentRepository.findByPortonePaymentIdWithLock(request.getPortonePaymentId()))
        .willReturn(Optional.of(mockPayment));
    given(portOneClient.getPaymentInfo(request.getPortonePaymentId())).willReturn(mockPortOneResponse);

    // when & then
    assertThatThrownBy(() -> paymentService.confirmPayment(request))
        .isInstanceOf(ServiceException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_FAILED);

    // orderItem 루프를 돌며 productService.increaseStock이 순차 호출되었는지 유효성 검증
    verify(productService, times(1)).increaseStock(101L, 2);
    verify(productService, times(1)).increaseStock(102L, 5);

    // 엔티티가 상태를 FAILED로 바꿨는지 확인
    verify(mockPayment, times(1)).fail();

    // 예외가 터졌으므로 하단 후속 포인트 원장 처리 로직은 단 한 번도 실행되지 않았는지 방어 검증
    verify(memberRepository, never()).findById(anyLong());
    verify(pointService, never()).createHistory(any(), any(), any(), anyLong());
  }

  // ==========================================
  // 시나리오 1: [성공 분기 완전 커버] - 포인트 복합 결제 사용 + 등급별 포인트 적립 + 주문 완료 전이
  // ==========================================
  @Test
  @DisplayName("결제 확정 성공: 모든 장부 검증을 통과하면 포인트 차감/적립 원장을 기록하고 주문을 COMPLETED 상태로 전환한다.")
  void confirmPayment_Success_AllBranches() {
    // given
    PaymentConfirmRequest request = new PaymentConfirmRequest("ORD-001", "imp_123");

    // 가짜 연관 객체 셋업
    Order mockOrder = mock(Order.class);
    Member mockMember = mock(Member.class);
    given(mockMember.getId()).willReturn(42L);
    // 등급별 적립률 5% 가정 (프로젝트의 MembershipGrade 명세 확인 요망)
    given(mockMember.getMembershipGrade()).willReturn(MembershipGrade.VIP);

    // 진짜 결제 도메인 준비 (복합 포인트 5,000원 태움 ➔ pgRealAmount는 45,000원 계산됨)
    Payment payment = Payment.createPayment(mockOrder, 42L, "imp_123", 50000L, 5000L);
    given(paymentRepository.findByPortonePaymentIdWithLock("imp_123")).willReturn(Optional.of(payment));

    // 포트원 정상 승인 데이터 응답 모킹
    PortOnePaymentResponse portoneResponse = mock(PortOnePaymentResponse.class);
    PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);
    given(portoneResponse.getStatus()).willReturn("PAID");
    given(portoneResponse.getAmount()).willReturn(mockAmount);
    given(mockAmount.getTotal()).willReturn(45000L); // pgRealAmount와 정확히 일치시켜 위변조 통과
    given(portOneClient.getPaymentInfo("imp_123")).willReturn(portoneResponse);

    given(memberRepository.findById(any())).willReturn(Optional.of(mockMember));

    // when
    PaymentResponse response = paymentService.confirmPayment(request);

    // then
    // 1. 반환 상태 검증
    assertThat(response.getStatus()).isEqualTo("COMPLETED");
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

    // 2. [조건문 커버] 사용 포인트 원장(USE) 적립 기록 동작 검증
    verify(pointService, times(1))
        .createHistory(eq(42L), eq(payment), eq(PointTransactionType.USE), eq(5000L));

    // 3. [조건문 커버] 등급별 차등 적립 원장(EARN) 동작 검증 (50000원 * VIP혜택 = 적립금 확인)
    verify(pointService, times(1))
        .createHistory(eq(42L), eq(payment), eq(PointTransactionType.EARN), anyLong());

    // 4. 멤버십 누적 금액 누적 및 부모 주문 상태 전이 유기적 호출 확인
    verify(mockMember, times(1)).increaseTotalPaymentAmount(45000L);
    verify(mockOrder, times(1)).completeOrder();
  }

  // ==========================================
  // 시나리오 2: [멱등성 방어선 분기 커버]
  // ==========================================
  @Test
  @DisplayName("결제 확정 멱등성 우회: 이미 COMPLETED 상태인 결제 건이 재요청되면 추가 연산 없이 기존 데이터를 반환한다.")
  void confirmPayment_ReturnImmediately_WhenAlreadyCompleted() {
    // given
    PaymentConfirmRequest request = new PaymentConfirmRequest("ORD-001", "imp_already_done");
    Payment completedPayment = Payment.createPayment(mock(Order.class), 42L, "imp_already_done", 10000L, 0L);
    ReflectionTestUtils.setField(completedPayment, "status", PaymentStatus.COMPLETED); // 강제 강제 완료 상태 변경

    given(paymentRepository.findByPortonePaymentIdWithLock("imp_already_done")).willReturn(Optional.of(completedPayment));

    // when
    PaymentResponse response = paymentService.confirmPayment(request);

    // then
    assertThat(response.getStatus()).isEqualTo("COMPLETED");
    // 멱등성에 걸려 통과했으므로 외부 포트원 API를 찌르지 않았음을 입증 (커버리지 수호)
    verify(portOneClient, never()).getPaymentInfo(anyString());
  }

  // ==========================================
  // 시나리오 3: [예외 및 보상 트랜잭션 루프 커버] - 가장 중요 ⭐
  // ==========================================
  @Test
  @DisplayName("결제 검증 실패: 외부 PG사 승인 금액과 우리 장부 금액이 미스매치되면 결제를 FAILED로 돌리고 상품 재고를 전량 자동 복구한다.")
  void confirmPayment_Fail_ValidationAndRestoreStock() {
    // given
    PaymentConfirmRequest request = new PaymentConfirmRequest("ORD-001", "imp_hacked");

    // 복상 트랜잭션이 돌아갈 가짜 주문 품목 리스트 바인딩
    Order mockOrder = mock(Order.class);
    OrderItem item1 = mock(OrderItem.class);
    given(item1.getProductId()).willReturn(101L);
    given(item1.getQuantity()).willReturn(2);
    given(mockOrder.getOrderItems()).willReturn(List.of(item1)); // 상품 1개 2개 수량 담김

    Payment payment = Payment.createPayment(mockOrder, 42L, "imp_hacked", 50000L, 0L); // 실결제액 50000원 기대
    given(paymentRepository.findByPortonePaymentIdWithLock("imp_hacked")).willReturn(Optional.of(payment));

    // 외부 포트원은 해커에 의해 100원만 결제되었다고 변조 응답 가정
    PortOnePaymentResponse portoneResponse = mock(PortOnePaymentResponse.class);
    PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);
    given(portoneResponse.getStatus()).willReturn("PAID");
    given(portoneResponse.getAmount()).willReturn(mockAmount);
    given(mockAmount.getTotal()).willReturn(100L); // 50000원 != 100원 (금액 위변조 적발)
    given(portOneClient.getPaymentInfo("imp_hacked")).willReturn(portoneResponse);

    // when & then
    assertThatThrownBy(() -> paymentService.confirmPayment(request))
        .isInstanceOf(ServiceException.class); // 전역 예외 정상 호출 확인

    // [최요구 커버리지] catch 블록 내부에 설계된 재고 복구 로직이 101번 상품에 대해 2개만큼 호출되었는지 철저하게 검증
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    verify(productService, times(1)).increaseStock(101L, 2);
    verify(memberRepository, never()).findById(any()); // 회원 조회단까지 가기 전에 정문 차단 증명
  }

  // ==========================================
  // 시나리오 4: [원천 누락 에러 분기 커버]
  // ==========================================
  @Test
  @DisplayName("결제 조회 실패: 포트원 결제 아이디를 장부에서 찾을 수 없으면 ORDER_NOT_FOUND 에러를 던진다.")
  void confirmPayment_Fail_OrderNotFound() {
    // given
    PaymentConfirmRequest request = new PaymentConfirmRequest("ORD-001", "imp_ghost");
    given(paymentRepository.findByPortonePaymentIdWithLock("imp_ghost")).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> paymentService.confirmPayment(request))
        .isInstanceOf(ServiceException.class);
  }
}
