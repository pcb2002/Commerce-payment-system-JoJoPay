package com.team11.jojopay.domain.member.entity;

import com.team11.jojopay.domain.member.enums.MembershipGrade;
import jakarta.persistence.*;
import java.time.LocalDateTime;
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
}
