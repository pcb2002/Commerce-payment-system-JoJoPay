package com.team11.jojopay.domain.subscription.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import com.team11.jojopay.domain.subscription.dto.request.SubscriptionStartRequest;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionBillingResponse;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.SubscriptionBillingStatus;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import com.team11.jojopay.domain.subscription.repository.BillingKeyRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionBillingRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionRepository;
import com.team11.jojopay.domain.subscription.validator.SubscriptionValidator;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

  private final SubscriptionRepository subscriptionRepository;
  private final BillingKeyRepository billingKeyRepository;
  private final SubscriptionBillingRepository subscriptionBillingRepository;

  private final MemberService memberService;
  private final PortOneClient portOneClient;
  private final SubscriptionValidator subscriptionValidator;
  private final SubscriptionTransactionService subscriptionTransactionService;

  @Transactional(readOnly = true)
  public SubscriptionResponse getMySubscription(Long memberId) {

    Subscription subscription = subscriptionRepository.findByMemberId(memberId)
        .orElseThrow(
            () -> new ServiceException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );

    return SubscriptionResponse.from(subscription);
  }

  /**
   * 첫 구독 결제를 수행하고 구독을 시작
   *
   * 첫 결제는 구독 생성 전에 수행하며, PortOne 응답의 결제 상태와 금액을 검증
   * 결제 검증에 성공한 경우에만 구독 생성, 첫 청구 이력 저장,
   * 포인트 적립, 누적 결제금액 반영을 별도 트랜잭션에서 처리
   */
  public SubscriptionResponse startSubscription(
      Long memberId,
      SubscriptionStartRequest request
  ) {
    Member member = memberService.findMemberById(memberId);

    // 활성 구독은 회원당 1개만 허용
    Optional<Subscription> activeSubscription =
        subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE);

    subscriptionValidator.validateNoActiveSubscription(activeSubscription);

    // 구독 시작에 사용할 결제수단이 로그인한 회원의 것인지 확인
    BillingKey billingKey = billingKeyRepository.findByIdAndMemberId(
        request.getBillingKeyId(),
        memberId
    ).orElseThrow(() -> new ServiceException(ErrorCode.BILLING_KEY_NOT_FOUND));

    // 삭제되었거나 비활성화된 결제수단으로 구독이 시작되지 않도록 검증
    subscriptionValidator.validateActiveBillingKey(billingKey);

    LocalDate startDate = LocalDate.now();
    LocalDate nextBillingDate = calculateNextBillingDate(startDate);

    // 첫 결제 식별자와 첫 이용 기간 생성
    String paymentId = createFirstPaymentId(memberId);
    String billingPeriod = createBillingPeriod(startDate);

    // 구독 생성 전 빌링키 기반 첫 결제 요청
    PortOnePaymentResponse response = portOneClient.scheduleBillingKeyPayment(
        billingKey.getCustomerUid(),
        paymentId,
        request.getPlan().getPrice(),
        request.getPlan().getPlanName()
    );

    // 첫 결제가 실제로 성공했는지 상태와 금액 검증
    subscriptionValidator.validateBillingPaymentResult(
        response,
        request.getPlan().getPrice()
    );

    // 결제 성공 후 구독 생성 및 DB 반영은 별도 트랜잭션에서 처리
    return subscriptionTransactionService.saveStartSubscriptionSuccess(
        member.getId(),
        billingKey.getId(),
        request.getPlan(),
        nextBillingDate,
        billingPeriod,
        paymentId
    );
  }

  @Transactional
  public SubscriptionResponse cancelSubscription(Long memberId) {

    Subscription subscription = subscriptionRepository.findByMemberIdAndStatus(
        memberId,
        SubscriptionStatus.ACTIVE
    ).orElseThrow(
        () -> new ServiceException(ErrorCode.NO_ACTIVE_SUBSCRIPTION)
    );

    subscription.cancel();

    return SubscriptionResponse.from(subscription);
  }

  // 현재 로그인한 회원의 구독을 조회한 뒤, 해당 구독의 결제 이력을 최신순으로 조회
  @Transactional(readOnly = true)
  public List<SubscriptionBillingResponse> getMySubscriptionBillings(Long memberId) {
    Subscription subscription = subscriptionRepository.findByMemberId(memberId)
        .orElseThrow(() -> new ServiceException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    return subscriptionBillingRepository
        .findAllBySubscriptionIdOrderByCreatedAtDesc(subscription.getId())
        .stream()
        .map(SubscriptionBillingResponse::from)
        .toList();
  }

  /**
   * 정기 결제를 수행하고 구독을 갱신
   *
   * 외부 결제 API 호출은 DB 트랜잭션 밖에서 수행
   * 결제 성공/실패에 따른 상태 변경은 SubscriptionTransactionService에서 별도 트랜잭션으로 처리
   */
  public void renewSubscription(Subscription subscription) {

    int nextCycle = calculateNextBillingCycle(subscription.getId());
    String paymentId = createRenewPaymentId(subscription.getId(), nextCycle);
    String billingPeriod = createBillingPeriod(subscription.getNextBillingDate());

    try {
      // 포트원 빌링키 결제 요청
      PortOnePaymentResponse response = portOneClient.scheduleBillingKeyPayment(
          subscription.getBillingKey().getCustomerUid(),
          paymentId,
          subscription.getPrice(),
          subscription.getPlan().getPlanName()
      );

      // 결제 상태와 금액이 기댓값과 일치하는지 검증
      subscriptionValidator.validateBillingPaymentResult(
          response,
          subscription.getPrice()
      );

      // 결제 성공 후 DB 반영은 별도 트랜잭션에서 처리
      subscriptionTransactionService.saveRenewSubscriptionSuccess(
          subscription.getId(),
          nextCycle,
          billingPeriod,
          paymentId
      );

    } catch (Exception e) {
      // 실패한 구독의 메타 정보(구독 ID)와 구체적인 예외를 로그에 남김
      log.error(
          "[구독 갱신 실패] 구독 ID: {}, 에러 메시지: {}",
          subscription.getId(),
          e.getMessage(),
          e);

      // 정기결제 실패 시 구독 상태는 즉시 변경하지 않고 실패 청구 이력만 저장
      subscriptionTransactionService.saveRenewSubscriptionFailure(
          subscription.getId(),
          nextCycle,
          billingPeriod
      );
    }
  }

  // 첫 구독 결제에 사용할 PortOne 결제 식별자를 생성
  private String createFirstPaymentId(Long memberId) {
    return "SUB_FIRST_" + memberId + "_" + System.currentTimeMillis();
  }

  // 정기결제는 같은 구독/회차에 대해 동일한 결제 식별자를 사용해 중복 결제 방어에 활용
  private String createRenewPaymentId(Long subscriptionId, int billingCycle) {
    return "SUB_RENEW_" + subscriptionId + "_" + billingCycle;
  }

  // 구독 이용 기간 문자열을 생성
  private String createBillingPeriod(LocalDate startDate) {
    return startDate + " ~ " + startDate.plusMonths(1).minusDays(1);
  }

  // 기준일로부터 다음 결제일을 계산
  private LocalDate calculateNextBillingDate(LocalDate baseDate) {
    return baseDate.plusMonths(1);
  }

  /**
   * 실패한 청구 이력은 회차 계산에서 제외
   * 성공한 정기 결제 이력 수를 기준으로 다음 결제 회차를 계산
    */
  private int calculateNextBillingCycle(Long subscriptionId) {
    return (int) subscriptionBillingRepository
        .countBySubscriptionIdAndBillingStatus(
            subscriptionId,
            SubscriptionBillingStatus.SUCCESS
        ) + 1;
  }
}
