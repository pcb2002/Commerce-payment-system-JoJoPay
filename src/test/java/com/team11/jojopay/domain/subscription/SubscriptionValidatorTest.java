package com.team11.jojopay.domain.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.BillingKeyStatus;
import com.team11.jojopay.domain.subscription.validator.SubscriptionValidator;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubscriptionValidatorTest {

  private final SubscriptionValidator validator = new SubscriptionValidator();

  @Test
  @DisplayName("포트원 결제 검증 에러 조건 완벽 엄호")
  void validateBillingPaymentResult_Fail_Scenarios() {
    // 1. 외부 PG 응답이 아예 누락된 null 라인 터치
    assertThatThrownBy(() -> validator.validateBillingPaymentResult(null, 15000L))
        .isInstanceOf(ServiceException.class)
        .satisfies(e -> {
          ServiceException se = (ServiceException) e;
          assertThat(se.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        });

    // 2. 상태 스펙이 PAID가 아니라 실패(FAILED)로 전이된 라인 터치
    PortOnePaymentResponse mockResponse1 = mock(PortOnePaymentResponse.class);
    given(mockResponse1.getStatus()).willReturn("FAILED");
    assertThatThrownBy(() -> validator.validateBillingPaymentResult(mockResponse1, 15000L))
        .isInstanceOf(ServiceException.class);

    // 3. 금액 위변조 저격선 터치 (기대값 15000원인데 PG 응답이 500원인 경우)
    PortOnePaymentResponse mockResponse2 = mock(PortOnePaymentResponse.class);
    PortOnePaymentResponse.Amount mockAmount = mock(PortOnePaymentResponse.Amount.class);
    given(mockResponse2.getStatus()).willReturn("PAID");
    given(mockResponse2.getAmount()).willReturn(mockAmount);
    given(mockAmount.getTotal()).willReturn(500L);

    assertThatThrownBy(() -> validator.validateBillingPaymentResult(mockResponse2, 15000L))
        .isInstanceOf(ServiceException.class);
  }

  @Test
  @DisplayName("중복 구독 방지 검증: 이미 가입된 구독 원장이 장부에 활성화 상태라면 ALREADY_ACTIVE_SUBSCRIPTION을 반환한다.")
  void validateNoActiveSubscription_Fail() {
    // given
    Subscription mockSubscription = mock(Subscription.class);
    Optional<Subscription> activeSubscription = Optional.of(mockSubscription);

    // when & then
    assertThatThrownBy(() -> validator.validateNoActiveSubscription(activeSubscription))
        .isInstanceOf(ServiceException.class)
        .satisfies(e -> {
          ServiceException se = (ServiceException) e;
          assertThat(se.getErrorCode()).isEqualTo(ErrorCode.ALREADY_ACTIVE_SUBSCRIPTION);
        });
  }

  @Test
  @DisplayName("빌링키 상태 검증: 만약 삭제(DELETED)된 카드로 정기 갱신을 찌르면 BILLING_KEY_NOT_FOUND 가 터진다.")
  void validateActiveBillingKey_Fail() {
    // given
    BillingKey mockBillingKey = mock(BillingKey.class);
    given(mockBillingKey.getStatus()).willReturn(BillingKeyStatus.DELETED);

    // when & then
    assertThatThrownBy(() -> validator.validateActiveBillingKey(mockBillingKey))
        .isInstanceOf(ServiceException.class)
        .satisfies(e -> {
          ServiceException se = (ServiceException) e;
          assertThat(se.getErrorCode()).isEqualTo(ErrorCode.BILLING_KEY_NOT_FOUND);
        });
  }
}
