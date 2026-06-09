package com.team11.jojopay.domain.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.entity.SubscriptionBilling;
import com.team11.jojopay.domain.subscription.enums.SubscriptionBillingStatus;
import com.team11.jojopay.domain.subscription.enums.SubscriptionPlan;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import com.team11.jojopay.domain.subscription.repository.BillingKeyRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionBillingRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionRepository;
import com.team11.jojopay.domain.subscription.service.SubscriptionTransactionService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class SubscriptionTransactionServiceTest {

  @InjectMocks
  private SubscriptionTransactionService subscriptionTransactionService;

  @Mock private MemberRepository memberRepository;
  @Mock private BillingKeyRepository billingKeyRepository;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private SubscriptionBillingRepository subscriptionBillingRepository;
  @Mock private PointService pointService;

  @Test
  @DisplayName("구독 시작 성공 저장 시 ACTIVE 구독과 첫 청구 이력이 생성되고 회원 누적 결제금액이 증가한다")
  void saveStartSubscriptionSuccess_createSubscriptionAndBilling() {
    Long memberId = 1L;
    Long billingKeyId = 10L;
    LocalDate nextBillingDate = LocalDate.of(2026, 7, 9);
    String billingPeriod = "2026-06-09 ~ 2026-07-08";
    String paymentId = "SUB_FIRST_1_20260609";

    // 실제 엔티티 상태 변경을 검증하기 위해 mock이 아닌 Member/BillingKey 엔티티 생성
    Member member = Member.signup("test", "test@example.com", "password", "010-1234-5678");
    ReflectionTestUtils.setField(member, "id", memberId);

    BillingKey billingKey = BillingKey.create(member, "customer-uid-1", "test-card", "1234-****-****-5678");
    ReflectionTestUtils.setField(billingKey, "id", billingKeyId);

    // 구독 요금과 회원 등급 적릴률을 기반으로 기대 적립 포인트를 계산
    Long expectedEarnPoint = SubscriptionPlan.BASIC.getPrice() * member.getMembershipGrade().getRewardRate() / 100;

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
    when(billingKeyRepository.findById(billingKeyId)).thenReturn(Optional.of(billingKey));

    // 구독 저장 시 DB에서 ID가 생성된 것처럼 id를 세팅해서 반환
    when(subscriptionRepository.save(org.mockito.ArgumentMatchers.any(Subscription.class)))
        .thenAnswer(invocation -> {
          Subscription subscription = invocation.getArgument(0);
          ReflectionTestUtils.setField(subscription, "id", 100L);
          return subscription;
        });

    // 구독 시작 성공 저장 로직을 실행
    SubscriptionResponse response = subscriptionTransactionService.saveStartSubscriptionSuccess(
        memberId,
        billingKeyId,
        SubscriptionPlan.BASIC,
        nextBillingDate,
        billingPeriod,
        paymentId
    );

    // 저장된 청구 이력을 캡쳐해서 billingCycle, 상태, 결제 ID를 검증
    ArgumentCaptor<SubscriptionBilling> billingCaptor =
        ArgumentCaptor.forClass(SubscriptionBilling.class);

    verify(subscriptionBillingRepository).save(billingCaptor.capture());

    // 포인트 적립과 회원 누적 결제금액 증가가 정상 반영됐는지 확인
    verify(pointService).createHistory(
        eq(memberId),
        isNull(),
        eq(PointTransactionType.EARN),
        eq(expectedEarnPoint)
    );

    SubscriptionBilling savedBilling = billingCaptor.getValue();

    assertThat(response.getSubscriptionId()).isEqualTo(100L);
    assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(response.getPrice()).isEqualTo(SubscriptionPlan.BASIC.getPrice());
    assertThat(response.getNextBillingDate()).isEqualTo(nextBillingDate);

    assertThat(savedBilling.getBillingCycle()).isEqualTo(1);
    assertThat(savedBilling.getBillingStatus()).isEqualTo(SubscriptionBillingStatus.SUCCESS);
    assertThat(savedBilling.getBillingPeriod()).isEqualTo(billingPeriod);
    assertThat(savedBilling.getPortoneTierPaymentId()).isEqualTo(paymentId);

    assertThat(member.getTotalPaymentAmount()).isEqualTo(SubscriptionPlan.BASIC.getPrice());
  }

  @Test
  @DisplayName("정기결제 성공 저장 시 청구 이력이 저장되고 다음 결제일과 회원 누적 결제금액이 갱신된다")
  void saveRenewSubscriptionSuccess() {
    Long memberId = 1L;
    Long subscriptionId = 100L;
    LocalDate currentNextBillingDate = LocalDate.of(2026, 6, 9);
    String billingPeriod = "2026-06-09 ~ 2026-07-08";
    String paymentId = "SUB_RENEW_100_2";

    Member member = Member.signup("test", "test@example.com", "password", "010-1234-5678");
    ReflectionTestUtils.setField(member, "id", memberId);

    BillingKey billingKey = BillingKey.create(member, "customer-uid-1", "test-card", "1234-****-****-5678");

    Subscription subscription = Subscription.start(
        member,
        billingKey,
        SubscriptionPlan.STANDARD,
        currentNextBillingDate
    );
    ReflectionTestUtils.setField(subscription, "id", subscriptionId);

    Long expectedEarnPoint = SubscriptionPlan.STANDARD.getPrice() * member.getMembershipGrade().getRewardRate() / 100;

    // 정기결제 갱신은 구독 row를 비관적 락으로 조회한 뒤 상태를 변경
    when(subscriptionRepository.findByIdWithPessimisticLock(subscriptionId))
        .thenReturn(Optional.of(subscription));

    subscriptionTransactionService.saveRenewSubscriptionSuccess(
        subscriptionId,
        2,
        billingPeriod,
        paymentId
    );

    ArgumentCaptor<SubscriptionBilling> billingCaptor =
        ArgumentCaptor.forClass(SubscriptionBilling.class);

    // 저장된 정기결제 청구 이력을 캡쳐해서 회차와 결제 정보를 검증
    verify(subscriptionBillingRepository).save(billingCaptor.capture());
    verify(pointService).createHistory(
        eq(memberId),
        isNull(),
        eq(PointTransactionType.EARN),
        eq(expectedEarnPoint)
    );

    SubscriptionBilling savedBilling = billingCaptor.getValue();

    assertThat(savedBilling.getBillingCycle()).isEqualTo(2);
    assertThat(savedBilling.getBillingStatus()).isEqualTo(SubscriptionBillingStatus.SUCCESS);
    assertThat(savedBilling.getAmount()).isEqualTo(SubscriptionPlan.STANDARD.getPrice());
    assertThat(savedBilling.getBillingPeriod()).isEqualTo(billingPeriod);
    assertThat(savedBilling.getPortoneTierPaymentId()).isEqualTo(paymentId);

    assertThat(subscription.getNextBillingDate()).isEqualTo(currentNextBillingDate.plusMonths(1));
    assertThat(member.getTotalPaymentAmount()).isEqualTo(SubscriptionPlan.STANDARD.getPrice());
  }

  @Test
  @DisplayName("구독 시작 저장 시 회원이 없으면 예외가 발생한다")
  void saveStartSubscriptionSuccess_fail_memberNotFound() {
    Long memberId = 1L;

    // 회원 조회 단계에서 실패하는 상황을 만듦
    when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

    // 회원이 없으면 MEMBER_NOT_FOUND 예외가 발생해야 함
    assertThatThrownBy(() -> subscriptionTransactionService.saveStartSubscriptionSuccess(
        memberId,
        10L,
        SubscriptionPlan.BASIC,
        LocalDate.of(2026, 7, 9),
        "2026-06-09 ~ 2026-07-08",
        "SUB_FIRST_1_20260609"
    ))
        .isInstanceOf(ServiceException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

    // 회원 조회에서 실패했으므로 이후 빌링키 조회, 구독 저장, 청구 저장, 포인트 적립은 일어나면 안됨
    verify(subscriptionRepository, never()).save(any());
    verify(billingKeyRepository, never()).findById(anyLong());
    verify(subscriptionBillingRepository, never()).save(any());
    verify(pointService, never()).createHistory(anyLong(), any(), any(), anyLong());
  }

  @Test
  @DisplayName("구독 시작 저장 시 빌링키가 없으면 예외가 발생한다")
  void saveStartSubscription_fail_billingKeyNotFound() {
    Long memberId = 1L;
    Long billingKeyId = 10L;

    Member member = Member.signup("test", "test@example.com", "password", "010-1234-5678");

    // 회원은 존재하지만 요청한 빌링키를 찾지 못하는 상황을 만듦
    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
    when(billingKeyRepository.findById(billingKeyId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> subscriptionTransactionService.saveStartSubscriptionSuccess(
        memberId,
        billingKeyId,
        SubscriptionPlan.BASIC,
        LocalDate.of(2026, 7, 9),
        "2026-06-09 ~ 2026-07-08",
        "SUB_FIRST_1_20260609"
    ))
        .isInstanceOf(ServiceException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.BILLING_KEY_NOT_FOUND);

    // 빌링키 조회에서 실패했으므로 구독 저장, 청구 저장, 포인트 적립은 일어나면 안됨
    verify(subscriptionRepository, never()).save(any());
    verify(subscriptionBillingRepository, never()).save(any());
    verify(pointService, never()).createHistory(anyLong(), any(), any(), anyLong());
  }

  @Test
  @DisplayName("정기결제 성공 저장 시 구독이 없으면 예외가 발생한다")
  void saveRenewSubscriptionSuccess_fail_subscriptionNotFound() {
    Long subscriptionId = 100L;

    // 정기결제 성공 반영 시 대상 구독을 찾지 못하는 상황을 만듦
    when(subscriptionRepository.findByIdWithPessimisticLock(subscriptionId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> subscriptionTransactionService.saveRenewSubscriptionSuccess(
        subscriptionId,
        2,
        "2026-06-09 ~ 2026-07-08",
        "SUB_RENEW_100_2"
    ))
        .isInstanceOf(ServiceException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.SUBSCRIPTION_NOT_FOUND);

    // 구독 조회에서 실패했으므로 청구 이력 저장과 포인트 적립은 일어나면 안됨
    verify(subscriptionBillingRepository, never()).save(any());
    verify(pointService, never()).createHistory(anyLong(), any(), any(), anyLong());
  }
}
