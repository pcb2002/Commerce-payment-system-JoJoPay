package com.team11.jojopay.domain.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import com.team11.jojopay.domain.subscription.dto.request.SubscriptionStartRequest;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.SubscriptionBillingStatus;
import com.team11.jojopay.domain.subscription.enums.SubscriptionPlan;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import com.team11.jojopay.domain.subscription.repository.BillingKeyRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionBillingRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionRepository;
import com.team11.jojopay.domain.subscription.validator.SubscriptionValidator;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

  @InjectMocks
  private SubscriptionService subscriptionService;

  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private BillingKeyRepository billingKeyRepository;
  @Mock private SubscriptionBillingRepository subscriptionBillingRepository;
  @Mock private MemberService memberService;
  @Mock private PortOneClient portOneClient;
  @Mock private SubscriptionValidator subscriptionValidator;
  @Mock private SubscriptionTransactionService subscriptionTransactionService;

  @Test
  @DisplayName("구독 시작 성공 시 결제 검증 후 구독 저장 처리를 위임한다")
  void startSubscription_success() {
    // given
    Long memberId = 1L;
    Long billingKeyId = 10L;
    LocalDate today = LocalDate.now();

    SubscriptionStartRequest request = mock(SubscriptionStartRequest.class);
    Member member = mock(Member.class);
    BillingKey billingKey = mock(BillingKey.class);
    PortOnePaymentResponse paymentResponse = mock(PortOnePaymentResponse.class);
    SubscriptionResponse subscriptionResponse = mock(SubscriptionResponse.class);

    given(request.getBillingKeyId()).willReturn(billingKeyId);
    given(request.getPlan()).willReturn(SubscriptionPlan.BASIC);
    given(member.getId()).willReturn(memberId);
    given(billingKey.getId()).willReturn(billingKeyId);
    given(billingKey.getCustomerUid()).willReturn("customer-uid-1");

    // 활성 구독이 없는 상태와 정상 빌링키 조회 상황을 만듦
    given(memberService.findMemberById(memberId)).willReturn(member);
    given(subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE))
        .willReturn(Optional.empty());
    given(billingKeyRepository.findByIdAndMemberId(billingKeyId, memberId))
        .willReturn(Optional.of(billingKey));

    // PortOne 결제가 성공했다고 가정하고, 이후 저장 로직은 TransactionService에 위임도되록 설정
    given(portOneClient.scheduleBillingKeyPayment(
        eq("customer-uid-1"),
        anyString(),
        eq(SubscriptionPlan.BASIC.getPrice()),
        eq(SubscriptionPlan.BASIC.getPlanName())
    )).willReturn(paymentResponse);
    given(subscriptionTransactionService.saveStartSubscriptionSuccess(
        eq(memberId),
        eq(billingKeyId),
        eq(SubscriptionPlan.BASIC),
        any(LocalDate.class),
        anyString(),
        anyString()
    )).willReturn(subscriptionResponse);

    // 실제 구독 시작 로직을 실행
    subscriptionService.startSubscription(memberId, request);

    ArgumentCaptor<LocalDate> nextBillingDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<String> billingPeriodCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> paymentIdCaptor = ArgumentCaptor.forClass(String.class);

    // 결제수단 검증, 결제 결과 검증, 구독 저장 위임이 순서대로 수행됐는지 확인
    verify(subscriptionValidator).validateActiveBillingKey(billingKey);
    verify(subscriptionValidator).validateBillingPaymentResult(
        paymentResponse,
        SubscriptionPlan.BASIC.getPrice()
    );
    verify(subscriptionTransactionService).saveStartSubscriptionSuccess(
        eq(memberId),
        eq(billingKeyId),
        eq(SubscriptionPlan.BASIC),
        nextBillingDateCaptor.capture(),
        billingPeriodCaptor.capture(),
        paymentIdCaptor.capture()
    );

    // 구독 시작 시 계산된 다음 결제일, 청구 기간, 결제 ID가 기대한 규칙을 따르는지 검증
    assertThat(nextBillingDateCaptor.getValue()).isEqualTo(today.plusMonths(1));
    assertThat(billingPeriodCaptor.getValue())
        .isEqualTo(today + " ~ " + today.plusMonths(1).minusDays(1));
    assertThat(paymentIdCaptor.getValue()).startsWith("SUB_FIRST_" + memberId + "_");
  }

  @Test
  @DisplayName("이미 활성 구독이 있으면 구독 시작에 실패하고 결제 요청을 보내지 않는다")
  void startSubscription_fail_alreadyActive() {
    Long memberId = 1L;

    SubscriptionStartRequest request = mock(SubscriptionStartRequest.class);
    Member member = mock(Member.class);
    Subscription existingSubscription = mock(Subscription.class);

    given(memberService.findMemberById(memberId)).willReturn(member);

    // 이미 ACTIVE 구독이 있는 상황을 만듦
    given(subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE))
        .willReturn(Optional.of(existingSubscription));

    // Validator가 활성 구독 중복 예외를 던지도록 설정
    doThrow(new ServiceException(ErrorCode.ALREADY_ACTIVE_SUBSCRIPTION))
        .when(subscriptionValidator)
            .validateNoActiveSubscription(any());

    assertThatThrownBy(() -> subscriptionService.startSubscription(memberId, request))
        .isInstanceOf(ServiceException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ALREADY_ACTIVE_SUBSCRIPTION);

    // 예외 발생 후에는 외부 결제 요청과 구독 저장 위임이 호출되면 안됨
    verify(portOneClient, never())
        .scheduleBillingKeyPayment(anyString(), anyString(), anyLong(), anyString());
    verify(subscriptionTransactionService, never()).saveStartSubscriptionSuccess(
        anyLong(), anyLong(), any(), any(), anyString(), anyString()
    );
  }

  @Test
  @DisplayName("정기결제 성공 시 결제 검증 후 갱신 저장 처리를 위임한다")
  void renewSubscription_success() {
    Subscription subscription = mock(Subscription.class);
    BillingKey billingKey = mock(BillingKey.class);
    PortOnePaymentResponse paymentResponse = mock(PortOnePaymentResponse.class);

    given(subscription.getId()).willReturn(55L);
    given(subscription.getBillingKey()).willReturn(billingKey);
    given(subscription.getPrice()).willReturn(SubscriptionPlan.STANDARD.getPrice());
    given(subscription.getPlan()).willReturn(SubscriptionPlan.STANDARD);
    given(subscription.getNextBillingDate()).willReturn(LocalDate.of(2026, 6, 9));
    given(billingKey.getCustomerUid()).willReturn("billing-key-123");

    // 기존 청구 이력이 1건 있는 상황을 만들어 다음 회차가 2회차로 계산되게 함
    given(subscriptionBillingRepository.countBySubscriptionIdAndBillingStatus(
        55L,
        SubscriptionBillingStatus.SUCCESS
    )).willReturn(1L);

    // 정기결제 성공 응답 준비
    given(portOneClient.scheduleBillingKeyPayment(
        eq("billing-key-123"),
        anyString(),
        eq(SubscriptionPlan.STANDARD.getPrice()),
        eq(SubscriptionPlan.STANDARD.getPlanName())
    )).willReturn(paymentResponse);

    // 정기결제 실행
    subscriptionService.renewSubscription(subscription);

    ArgumentCaptor<Integer> cycleCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> billingPeriodCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> paymentIdCaptor = ArgumentCaptor.forClass(String.class);

    // 결제 검증 후 성공 저장 처리가 위임됐는지 확인
    verify(subscriptionValidator).validateBillingPaymentResult(
        paymentResponse,
        SubscriptionPlan.STANDARD.getPrice()
    );
    verify(subscriptionTransactionService).saveRenewSubscriptionSuccess(
        eq(55L),
        cycleCaptor.capture(),
        billingPeriodCaptor.capture(),
        paymentIdCaptor.capture()
    );

    assertThat(cycleCaptor.getValue()).isEqualTo(2);
    assertThat(billingPeriodCaptor.getValue()).isEqualTo("2026-06-09 ~ 2026-07-08");
    assertThat(paymentIdCaptor.getValue()).isEqualTo("SUB_RENEW_55_2");
  }

  @Test
  @DisplayName("정기결제 실패 시 실패 청구 이력 저장 처리를 위임한다")
  void renewSubscription_fail_portOneException() {
    Subscription subscription = mock(Subscription.class);
    BillingKey billingKey = mock(BillingKey.class);

    given(subscription.getId()).willReturn(55L);
    given(subscription.getBillingKey()).willReturn(billingKey);
    given(subscription.getPrice()).willReturn(SubscriptionPlan.STANDARD.getPrice());
    given(subscription.getPlan()).willReturn(SubscriptionPlan.STANDARD);
    given(subscription.getNextBillingDate()).willReturn(LocalDate.of(2026, 6, 9));
    given(billingKey.getCustomerUid()).willReturn("billing-key-123");

    given(subscriptionBillingRepository.countBySubscriptionIdAndBillingStatus(
        55L,
        SubscriptionBillingStatus.SUCCESS
    )).willReturn(1L);

    // PortOne 결제 요청에서 예외가 발생하는 상황을 만듦
    doThrow(new RuntimeException("PortOne 결제 실패"))
        .when(portOneClient)
        .scheduleBillingKeyPayment(anyString(), anyString(), anyLong(), anyString());

    // 실패해도 renewSubscription 내부에서 예외를 잡고 실패 이력 저장으로 위임
    subscriptionService.renewSubscription(subscription);

    ArgumentCaptor<Integer> cycleCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> billingPeriodCaptor = ArgumentCaptor.forClass(String.class);

    // 실패 청구 이력 저장은 호출되고, 성공 저장은 호출되지 않아야 함
    verify(subscriptionTransactionService).saveRenewSubscriptionFailure(
        eq(55L),
        cycleCaptor.capture(),
        billingPeriodCaptor.capture()
    );
    verify(subscriptionTransactionService, never()).saveRenewSubscriptionSuccess(
        anyLong(),
        anyInt(),
        anyString(),
        anyString()
    );

    assertThat(cycleCaptor.getValue()).isEqualTo(2);
    assertThat(billingPeriodCaptor.getValue()).isEqualTo("2026-06-09 ~ 2026-07-08");
  }
}