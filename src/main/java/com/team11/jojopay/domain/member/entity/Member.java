package com.team11.jojopay.domain.member.entity;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.enums.MembershipGrade;
import com.team11.jojopay.domain.point.entity.PointHistory;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "phone_number", nullable = false, length = 20)
  private String phoneNumber;

  @Column(nullable = false)
  private Long pointBalance;

  @Column(nullable = false)
  private Long totalPaymentAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MembershipGrade membershipGrade;

  @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PointHistory> pointHistories = new ArrayList<>();

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private Member(String name, String email, String passwordHash, String phoneNumber) {
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.phoneNumber = phoneNumber;
    this.pointBalance = 0L;
    this.totalPaymentAmount = 0L;
    this.membershipGrade = MembershipGrade.NORMAL;
  }

  public static Member signup(String name, String email, String passwordHash, String phoneNumber) {
    return new Member(
        name,
        email,
        passwordHash,
        phoneNumber
    );
  }

  // 결제 완료 시 누적 결제 금액을 증가시키고 등급을 재계산
  public void increaseTotalPaymentAmount(Long amount) {
    this.totalPaymentAmount += amount;
    updateMembershipGrade();
  }

  // 환불 완료 시 누적 결제 금액을 감소시키고 등급을 재계산
  public void decreaseTotalPaymentAmount(Long amount) {
    this.totalPaymentAmount = Math.max(0L, totalPaymentAmount - amount);
    updateMembershipGrade();
  }

  // 포인트 적립 또는 사용 포인트 복구 시 포인트 잔액을 증가
  public void addPoint(Long point) {
    this.pointBalance += point;
  }

  // 포인트 사용 또는 적립 포인트 회수 시 포인트 잔액을 차감
  public void usePoint(Long point) {
    if (this.pointBalance < point) {
      throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE);
    }
    this.pointBalance -= point;
  }

  // 누적 결제 금액 기준으로 멤버십 등급을 갱신
  private void updateMembershipGrade() {
    if (this.totalPaymentAmount >= MembershipGrade.VVIP.getMinimumAmount()) {
      this.membershipGrade = MembershipGrade.VVIP;
      return;
    }

    if (this.totalPaymentAmount >= MembershipGrade.VIP.getMinimumAmount()) {
      this.membershipGrade = MembershipGrade.VIP;
      return;
    }

    this.membershipGrade = MembershipGrade.NORMAL;
  }
}
