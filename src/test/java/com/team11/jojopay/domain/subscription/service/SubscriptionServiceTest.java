package com.team11.jojopay.domain.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.enums.MembershipGrade;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.subscription.dto.request.SubscriptionStartRequest;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.entity.SubscriptionBilling;
import com.team11.jojopay.domain.subscription.enums.BillingKeyStatus;
import com.team11.jojopay.domain.subscription.enums.SubscriptionPlan;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import com.team11.jojopay.domain.subscription.repository.BillingKeyRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionBillingRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionRepository;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  @Mock private PointService pointService;

  @Test
  @DisplayName("구독 시작 성공: 최초 구독 신청 시 1회차 성공 이력이 저장되고 포인트가 적립된다.")
  void startSubscription_Success() {
    // given
    Long memberId = 1L;
    SubscriptionStartRequest request = mock(SubscriptionStartRequest.class);
    given(request.getBillingKeyId()).willReturn(10L);
    given(request.getPlan()).willReturn(SubscriptionPlan.BASIC); // 9,900원 플랜

    Member mockMember = mock(Member.class);
    BillingKey mockBillingKey = mock(BillingKey.class);
    Subscription mockSubscription = mock(Subscription.class);

    given(mockMember.getId()).willReturn(memberId);
    given(mockMember.getMembershipGrade()).willReturn(MembershipGrade.NORMAL); // 적립률 스펙 정보 제공 가정
    given(mockBillingKey.getStatus()).willReturn(BillingKeyStatus.ACTIVE);
    given(mockBillingKey.getCustomerUid()).willReturn("user_billing_uid_123");
    given(mockSubscription.getPrice()).willReturn(9900L);
    given(mockSubscription.getPlan()).willReturn(SubscriptionPlan.BASIC);

    // 가짜 객체 반환 셋업
    given(memberService.findMemberById(memberId)).willReturn(mockMember);
    // 중복 구독이 없는 상태 조회 세팅 (Optional.empty())
    given(subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE))
        .willReturn(Optional.empty());
    given(billingKeyRepository.findByIdAndMemberId(10L, memberId)).willReturn(Optional.of(mockBillingKey));
    given(subscriptionRepository.save(any(Subscription.class))).willReturn(mockSubscription);

    // when
    SubscriptionResponse response = subscriptionService.startSubscription(memberId, request);

    // then
    // 포트원 첫 결제 호출 검증
    verify(portOneClient, times(1)).scheduleBillingKeyPayment(
        eq("user_billing_uid_123"), anyString(), eq(9900L), eq("베이직")
    );
    // 1회차 웰컴 성공 이력 적재 검증
    verify(subscriptionBillingRepository, times(1)).save(any(SubscriptionBilling.class));
    // 포인트 적립 메서드가 정확히 member.getId() 규격으로 호출되었는지 검증
    verify(pointService, times(1)).createHistory(eq(memberId), eq(null), eq(PointTransactionType.EARN), anyLong());
    // 회원 누적 결제액 업데이트 호출 검증
    verify(mockMember, times(1)).increaseTotalPaymentAmount(9900L);

    assertThat(response).isNotNull();
  }

  @Test
  @DisplayName("구독 시작 실패: 이미 활성화된(ACTIVE) 구독이 존재하면 예외가 터진다.")
  void startSubscription_Fail_AlreadyActive() {
    // given
    Long memberId = 1L;
    SubscriptionStartRequest request = mock(SubscriptionStartRequest.class);
    Member mockMember = mock(Member.class);
    Subscription existingSubscription = mock(Subscription.class);

    given(memberService.findMemberById(memberId)).willReturn(mockMember);
    // ❌ 이미 구독이 존재하는 상태 유도
    given(subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE))
        .willReturn(Optional.of(existingSubscription));

    // when & then
    assertThatThrownBy(() -> subscriptionService.startSubscription(memberId, request))
        .isInstanceOf(ServiceException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ALREADY_ACTIVE_SUBSCRIPTION);

    // 결제 및 후속 영속성 저장은 절대 실행되지 않아야 함
    verify(portOneClient, never()).scheduleBillingKeyPayment(any(), any(), anyLong(), any());
    verify(subscriptionBillingRepository, never()).save(any());
  }

  @Test
  @DisplayName("구독 갱신 성공: 스케줄러에 의한 정기 결제 성공 시 차기 회차 성공 원장이 적재되고 다음 결제일이 이월된다.")
  void renewSubscription_Success() {
    // given
    Subscription mockSubscription = mock(Subscription.class);
    Member mockMember = mock(Member.class);
    BillingKey mockBillingKey = mock(BillingKey.class);

    given(mockSubscription.getId()).willReturn(55L);
    given(mockSubscription.getMember()).willReturn(mockMember);
    given(mockSubscription.getBillingKey()).willReturn(mockBillingKey);
    given(mockSubscription.getPrice()).willReturn(19900L); // 스탠다드 플랜 가정
    given(mockSubscription.getPlan()).willReturn(SubscriptionPlan.STANDARD);
    given(mockSubscription.getNextBillingDate()).willReturn(LocalDate.now());

    given(mockMember.getId()).willReturn(7L);
    given(mockMember.getMembershipGrade()).willReturn(MembershipGrade.NORMAL);
    given(mockBillingKey.getCustomerUid()).willReturn("billing_key_777");

    // 회차 계산을 위한 비어있는 리스트 혹은 기존 리스트 모킹
    given(subscriptionBillingRepository.findAllBySubscriptionIdOrderByCreatedAtDesc(55L))
        .willReturn(List.of()); // 비어있으면 0 + 1 = 2회차로 원장 적재 작동

    // when
    subscriptionService.renewSubscription(mockSubscription);

    // then
    // 1. 포트원 자동 결제 정상 상신 검증
    verify(portOneClient, times(1)).scheduleBillingKeyPayment(
        eq("billing_key_777"), anyString(), eq(19900L), eq("스탠다드")
    );
    // 2. 갱신 성공 원장 저장 검증
    verify(subscriptionBillingRepository, times(1)).save(any(SubscriptionBilling.class));
    // 3. 적립 원장 연동 검증
    verify(pointService, times(1)).createHistory(eq(7L), eq(null), eq(PointTransactionType.EARN), anyLong());
    // 4. 다음 결제일 1달 이월 행위 검증
    verify(mockSubscription, times(1)).updateNextBillingDate();
  }

  @Test
  @DisplayName("구독 갱신 실패: 외부 결제 API 예외 발생 시, 에러가 삼켜지며 실패 원장이 적재되고 미납(PAST_DUE) 상태로 전이된다.")
  void renewSubscription_Fail_PortOneException() {
    // given
    Subscription mockSubscription = mock(Subscription.class);
    Member mockMember = mock(Member.class);
    BillingKey mockBillingKey = mock(BillingKey.class);

    given(mockSubscription.getId()).willReturn(55L);
    given(mockSubscription.getMember()).willReturn(mockMember);
    given(mockSubscription.getBillingKey()).willReturn(mockBillingKey);
    given(mockSubscription.getPrice()).willReturn(19900L);
    given(mockSubscription.getPlan()).willReturn(SubscriptionPlan.STANDARD);
    given(mockSubscription.getNextBillingDate()).willReturn(LocalDate.now());

    given(mockMember.getId()).willReturn(7L);
    given(mockBillingKey.getCustomerUid()).willReturn("billing_key_777");

    // 포트원 결제 시 한도 초과 등의 이유로 런타임 예외 발생 시뮬레이션
    doThrow(new RuntimeException("포트원 결제 한도 초과 에러"))
        .when(portOneClient).scheduleBillingKeyPayment(any(), any(), anyLong(), any());

    // when
    // 로직 내부의 try-catch 덕분에 배치가 터지지 않고 부드럽게 Exception을 삼키며 마감되어야 함
    subscriptionService.renewSubscription(mockSubscription);

    // then
    // 결제 시도는 일어났는지 확인
    verify(portOneClient, times(1)).scheduleBillingKeyPayment(any(), any(), anyLong(), any());
    // 실패 이력이 정상적으로 생성되어 디비에 세이브되었는지 확인
    verify(subscriptionBillingRepository, times(1)).save(any(SubscriptionBilling.class));
    // 엔티티가 미납 마크로 전이되었는지 상태 검증
    verify(mockSubscription, times(1)).markAsPastDue();

    // 결제가 실패했으므로 포인트 적립이나 다음 결제일 이월은 절대 실행 X
    verify(pointService, never()).createHistory(anyLong(), any(), any(), anyLong());
    verify(mockSubscription, never()).updateNextBillingDate();
  }
}