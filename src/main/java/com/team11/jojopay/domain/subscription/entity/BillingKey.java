package com.team11.jojopay.domain.subscription.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.subscription.enums.BillingKeyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "billing_key")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingKey extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "customer_uid", nullable = false, unique = true, length = 255)
  private String customerUid;

  @Column(name = "card_name", nullable = false, length = 50)
  private String cardName;

  @Column(name = "card_number", nullable = false, length = 20)
  private String cardNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BillingKeyStatus status;

  private BillingKey(
      Member member,
      String customerUid,
      String cardName,
      String cardNumber
  ) {
    this.member = member;
    this.customerUid = customerUid;
    this.cardName = cardName;
    this.cardNumber = cardNumber;
    this.status = BillingKeyStatus.ACTIVE;
  }

  public static BillingKey create(
      Member member,
      String customerUid,
      String cardName,
      String cardNumber
  ) {
    return new BillingKey(member, customerUid, cardName, cardNumber);
  }

  /**
   * 결제수단을 물리 삭제하지 않고 비활성화 처리
   * 상태값만 DELETED로 변경
   */
  public void delete() {
    this.status = BillingKeyStatus.DELETED;
  }
}
