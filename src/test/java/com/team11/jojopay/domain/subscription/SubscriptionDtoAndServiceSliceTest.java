package com.team11.jojopay.domain.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.subscription.dto.request.SubscriptionStartRequest;
import com.team11.jojopay.domain.subscription.enums.SubscriptionPlan;
import com.team11.jojopay.domain.subscription.repository.BillingKeyRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionBillingRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionRepository;
import com.team11.jojopay.domain.subscription.service.SubscriptionService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SubscriptionDtoAndServiceSliceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==========================================
    // 1. Request DTO 내장 필드 검증 (커버리지 스코어 흡수)
    // ==========================================
    @Test
    @DisplayName("SubscriptionStartRequest 직렬화 및 Getter 작동선 커버")
    void subscriptionStartRequest_Coverage_Test() throws Exception {
        // given: JSON 명세 수동 바인딩
        String json = "{\"billingKeyId\":99,\"plan\":\"STANDARD\"}";

        // when
        SubscriptionStartRequest request = objectMapper.readValue(json, SubscriptionStartRequest.class);

        // then
        assertThat(request.getBillingKeyId()).isEqualTo(99L);
        assertThat(request.getPlan()).isEqualTo(SubscriptionPlan.STANDARD);
    }

    // ==========================================
    // 2. 서비스 레이어 - 예외 분기선 다이렉트 타격 (커버리지 폭발 구간)
    // ==========================================
    @Test
    @DisplayName("구독 단건 조회 실패 예외 커버: 장부에 구독 원장이 없는 회원이면 SUBSCRIPTION_NOT_FOUND 가 발생한다.")
    void getMySubscription_Fail_NotFound() {
        // given: 복잡한 인프라 대신 레포지토리만 가짜로 Mocking 하여 서비스 레이어 껍데기 구동
        SubscriptionRepository mockRepo = mock(SubscriptionRepository.class);
        given(mockRepo.findByMemberId(any())).willReturn(Optional.empty());

        // 핵심 로직과 무관한 나머지 의존성은 전부 mock 처리하여 주입
        SubscriptionService subscriptionService = new SubscriptionService(
                mockRepo, mock(BillingKeyRepository.class), mock(SubscriptionBillingRepository.class),
                null, null, null, null
        );

        // when & then: 0원인 상태에서 서비스 비즈니스 코드 라인을 다이렉트로 밟고 터뜨려 잔여 수치를 회수합니다.
        assertThatThrownBy(() -> subscriptionService.getMySubscription(1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("구독 취소 실패 예외 커버: 해지할 활성 구독권(ACTIVE)이 장부에 존재하지 않는다면 NO_ACTIVE_SUBSCRIPTION이 발생한다.")
    void cancelSubscription_Fail_NoActive() {
        // given
        SubscriptionRepository mockRepo = mock(SubscriptionRepository.class);
        given(mockRepo.findByMemberIdAndStatus(any(), any())).willReturn(Optional.empty());

        SubscriptionService subscriptionService = new SubscriptionService(
                mockRepo, mock(BillingKeyRepository.class), mock(SubscriptionBillingRepository.class),
                null, null, null, null
        );

        // when & then: 서비스 내부 cancelSubscription의 예외 throw 구문을 강제로 통과시킵니다.
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.NO_ACTIVE_SUBSCRIPTION);
                });
    }

    @Test
    @DisplayName("구독 결제 내역 조회 실패 예외 커버: 조회 타겟 유저가 구독 자체를 한 적이 없다면 SUBSCRIPTION_NOT_FOUND 가 발생한다.")
    void getMySubscriptionBillings_Fail_NotFound() {
        // given
        SubscriptionRepository mockRepo = mock(SubscriptionRepository.class);
        given(mockRepo.findByMemberId(any())).willReturn(Optional.empty());

        SubscriptionService subscriptionService = new SubscriptionService(
                mockRepo, mock(BillingKeyRepository.class), mock(SubscriptionBillingRepository.class),
                null, null, null, null
        );

        // when & then: 서비스 내부 getMySubscriptionBillings의 첫 방어선 라인 수집
        assertThatThrownBy(() -> subscriptionService.getMySubscriptionBillings(1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_NOT_FOUND);
                });
    }
}
