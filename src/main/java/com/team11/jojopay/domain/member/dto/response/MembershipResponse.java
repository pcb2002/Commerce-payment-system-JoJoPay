package com.team11.jojopay.domain.member.dto.response;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.enums.MembershipGrade;
import lombok.Getter;

@Getter
public class MembershipResponse {

  private final MembershipGrade membershipGrade;

  private final Long totalPaymentAmount;

  // 포인트 적립률
  private final int rewardRate;

  // 다음 등급까지 남은 금액
  private final Long nextGradeRemainingAmount;

  private MembershipResponse(
      MembershipGrade membershipGrade,
      Long totalPaymentAmount,
      int rewardRate,
      Long nextGradeRemainingAmount
  ) {
    this.membershipGrade = membershipGrade;
    this.totalPaymentAmount = totalPaymentAmount;
    this.rewardRate = rewardRate;
    this.nextGradeRemainingAmount = nextGradeRemainingAmount;
  }

  public static MembershipResponse from(Member member) {
    MembershipGrade grade = member.getMembershipGrade();
    Long totalPaymentAmount = member.getTotalPaymentAmount();

    return new MembershipResponse(
        grade,
        totalPaymentAmount,
        grade.getRewardRate(),
        grade.calculateNextGradeRemainingAmount(totalPaymentAmount)
    );
  }

}
