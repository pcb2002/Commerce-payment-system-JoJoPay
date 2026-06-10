package com.team11.jojopay.domain.member.dto.response;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.enums.MembershipGrade;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class MemberResponse {

  private final String name;

  private final String email;

  private final String phoneNumber;

  private final MembershipGrade membershipGrade;

  private final Long pointBalance;

  private final Long totalPaymentAmount;

  private final LocalDateTime createdAt;

  private MemberResponse(
      String name,
      String email,
      String phoneNumber,
      MembershipGrade membershipGrade,
      Long pointBalance,
      Long totalPaymentAmount,
      LocalDateTime createdAt
  ) {
    this.name = name;
    this.email = email;
    this.phoneNumber = phoneNumber;
    this.membershipGrade = membershipGrade;
    this.pointBalance = pointBalance;
    this.totalPaymentAmount = totalPaymentAmount;
    this.createdAt = createdAt;
  }

  public static MemberResponse from(Member member) {
    return new MemberResponse(
        member.getName(),
        member.getEmail(),
        member.getPhoneNumber(),
        member.getMembershipGrade(),
        member.getPointBalance(),
        member.getTotalPaymentAmount(),
        member.getCreatedAt()
    );
  }
}
