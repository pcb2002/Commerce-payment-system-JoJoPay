package com.team11.jojopay.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
}
