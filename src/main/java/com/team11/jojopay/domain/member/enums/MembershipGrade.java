package com.team11.jojopay.domain.member.enums;

import lombok.Getter;

@Getter
public enum MembershipGrade {
  NORMAL(0L, 1),
  VIP(50000L, 5),
  VVIP(100000L, 10);

  // 해당 등급이 되기 위한 최소 누적 결제 금액
  private final Long minimumAmount;

  private final int rewardRate;

  MembershipGrade(Long minimumAmount, int rewardRate) {
    this.minimumAmount = minimumAmount;
    this.rewardRate = rewardRate;
  }

  /**
   * 현재 누적 결제 금액을 기준으로 다음 등급까지 남은 금액 계산
   * 최고 등급인 경우 0을 반환
   */
  public Long calculateNextGradeRemainingAmount(Long totalPaymentAmount) {
    if (this == NORMAL) {
      return Math.max(0L, VIP.minimumAmount - totalPaymentAmount);
    }

    if (this == VIP) {
      return Math.max(0L, VVIP.minimumAmount - totalPaymentAmount);
    }

    return 0L;
  }
}
