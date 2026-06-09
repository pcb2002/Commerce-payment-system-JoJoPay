package com.team11.jojopay.domain.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionBillingResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.entity.SubscriptionBilling;
import com.team11.jojopay.domain.subscription.enums.SubscriptionBillingStatus;
import com.team11.jojopay.domain.subscription.enums.SubscriptionPlan;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SubscriptionEntityAndDtoTest {

    // ==========================================
    // 1. Subscription 엔티티 도메인 로직 완전 커버
    // ==========================================
    @Test
    @DisplayName("구독 엔티티 기본 제어권 검증: start 팩토리 메서드 가동 시 ACTIVE 상태와 플랜 정가가 알맞게 부여된다.")
    void subscription_Domain_Logic_Test() {
        // given
        Member mockMember = mock(Member.class);
        BillingKey mockBillingKey = mock(BillingKey.class);
        LocalDate nextBilling = LocalDate.of(2026, 7, 1);

        // when: 정적 팩토리 메서드 라인 터치
        Subscription subscription = Subscription.start(mockMember, mockBillingKey, SubscriptionPlan.PREMIUM, nextBilling);

        // then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getPrice()).isEqualTo(29900L); // 프리미엄 29,900원 명세 검증
        assertThat(subscription.getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);

        // 미납 전이 메서드 터치
        subscription.markAsPastDue();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);

        // 자동 결제 성공 시 1달 뒤로 이월되는 날짜 연산 메서드 터치
        subscription.updateNextBillingDate();
        assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 8, 1)); // 7월 1일 -> 8월 1일 자동 이월
    }

    // ==========================================
    // 2. SubscriptionBilling 영속성 이력 생성 팩토리 커버
    // ==========================================
    @Test
    @DisplayName("구독 결제 실패 이력 엔티티 검증: createFailed 호출 시 FAILED 상태의 영속 규격이 완성된다.")
    void subscriptionBilling_Failed_Factory_Test() {
        // given
        Subscription mockSubscription = mock(Subscription.class);

        // when: 실패 전용 결제 이력 팩토리 라인 터치
        SubscriptionBilling failedBilling = SubscriptionBilling.createFailed(
                mockSubscription, 2, "2026-07-01 ~ 2026-07-31", 19900L
        );

        // then
        assertThat(failedBilling.getBillingStatus()).isEqualTo(SubscriptionBillingStatus.FAILED);
        assertThat(failedBilling.getPortoneTierPaymentId()).isNull(); // 실패 시 포트원 아이디 null 규격 검증
        assertThat(failedBilling.getAmount()).isEqualTo(19900L);
    }

    // ==========================================
    // 3. Response DTO 자바 맵핑 레이어 커버선 확보
    // ==========================================
    @Test
    @DisplayName("SubscriptionBillingResponse 변환 명세 검증: 엔티티 원장 필드가 응답 DTO 규격으로 100% 미러링된다.")
    void subscriptionBillingResponse_From_Entity_Test() {
        // given: 변환용 타겟 엔티티 메모리 조립
        SubscriptionBilling billingEntity = SubscriptionBilling.createSuccess(
                mock(Subscription.class), 1, "2026-06-09 ~ 2026-07-08", 29900L, "PORTONE_TX_777"
        );

        // BaseTimeEntity의 가상 생성일시 주입
        LocalDateTime dummyNow = LocalDateTime.now();
        ReflectionTestUtils.setField(billingEntity, "id", 999L);
        ReflectionTestUtils.setField(billingEntity, "createdAt", dummyNow);

        // when: DTO 내부의 .from() 정적 변환 레이어 관통
        SubscriptionBillingResponse response = SubscriptionBillingResponse.from(billingEntity);

        // then
        assertThat(response.getSubscriptionBillingId()).isEqualTo(999L);
        assertThat(response.getBillingStatus()).isEqualTo(SubscriptionBillingStatus.SUCCESS);
        assertThat(response.getPortoneTierPaymentId()).isEqualTo("PORTONE_TX_777");
        assertThat(response.getCreatedAt()).isEqualTo(dummyNow);
    }

    // ==========================================
    // 4. SubscriptionPlan Enum 내장 필드 커버
    // ==========================================
    @Test
    @DisplayName("SubscriptionPlan 열거형 명세 검증: 각 플랜 상수별 한글 명칭과 정가가 알맞게 꺼내지는가")
    void subscriptionPlan_Enum_Test() {
        // Enum 내부에 들어있는 원천 getter 분기선들을 싹 통과시킵니다.
        assertThat(SubscriptionPlan.BASIC.getPlanName()).isEqualTo("베이직");
        assertThat(SubscriptionPlan.BASIC.getPrice()).isEqualTo(9900L);

        assertThat(SubscriptionPlan.STANDARD.getPlanName()).isEqualTo("스탠다드");
        assertThat(SubscriptionPlan.STANDARD.getPrice()).isEqualTo(19900L);
    }
}
