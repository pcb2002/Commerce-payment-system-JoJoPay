package com.team11.jojopay.domain.payment.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.payment.dto.request.PaymentConfirmRequest;
import com.team11.jojopay.domain.payment.dto.response.PaymentResponse;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.product.service.ProductService;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final MemberRepository memberRepository;
  private final PortOneClient portOneClient;
  private final ProductService productService; // 상품 정보 조회용 서비스
  private final PointService pointService; // 포인트 적립용 서비스

  /**
   * 결제 확정 및 비즈니스 로직(포인트 처리)을 수행합니다.
   */
  @Transactional
  public PaymentResponse confirmPayment(PaymentConfirmRequest request) {
    // 기존 결제 정보 조회
    Payment payment = paymentRepository.findByPortonePaymentIdWithLock(request.getPortonePaymentId())
        .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND));

    // 이미 완료된 결제인지 체크
    if (payment.getStatus() == PaymentStatus.COMPLETED) {
      return PaymentResponse.from(payment); // 이미 완료된 결제는 그대로 반환
    }

    // 포트원 API 교차 검증
    PortOnePaymentResponse portoneData = portOneClient.getPaymentInfo(request.getPortonePaymentId());
    Order order = payment.getOrder();

    try {
            validatePortOneStatus(payment, portoneData);
        } catch (ServiceException e) {
            // [수정] 주문의 모든 상품에 대해 재고 복구 수행 (OrderItem 루프)
            order.getOrderItems().forEach(orderItem ->
                productService.increaseStock(orderItem.getProductId(), orderItem.getQuantity())
            );
            throw e;
        }

    // 연관된 객체(Member) 정보 가져오기
    Member member = memberRepository.findById(order.getMemberId())
                .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));

    // 포인트 복합 결제 처리 (사용 원장 통합 적용)
    if (payment.getUsedPoint() > 0) {
    pointService.createHistory(member, payment, PointTransactionType.USE, payment.getUsedPoint());
    }

    // 등급별 포인트 차등 적립 정책 구현 (적립 원장 통합 적용)
    double earnRate = member.getMembershipGrade().getRewardRate();
    Long earnPoint = (long) (payment.getAmount() * earnRate);

    if (earnPoint > 0) {
      pointService.createHistory(member, payment, PointTransactionType.EARN, earnPoint);
    }

    // [멤버십]  누적 결제 금액 업데이트 및 등급 자동 갱신
    member.increaseTotalPaymentAmount(payment.getPgRealAmount());

    // 최종 결제 상태 완료 처리 및 승인 시간 기록
    payment.complete();
    if (payment.getOrder() != null) {
      payment.getOrder().completeOrder(); // 주문 엔티티의 상태도 완료로 변경
    }

    return PaymentResponse.from(payment);
  }

  /**
   * 포트원 응답 데이터와 우리 DB 데이터를 비교 검증합니다.
   */
  private void validatePortOneStatus(Payment payment, PortOnePaymentResponse portoneData) {
    // 결제 상태가 'PAID'가 아니거나 금액이 다르면 예외 발생
    if (!"PAID".equals(portoneData.getStatus()) ||
        !payment.getPgRealAmount().equals(portoneData.getAmount().getTotal())) {
      payment.fail(); // 엔티티 상태 변경
      throw new ServiceException(ErrorCode.VALIDATION_FAILED);
    }
  }
}
