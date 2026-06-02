package com.team11.jojopay.domain.point.entity;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "POINT_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id") // 결제 건과 연관 (환불이나 충전더미 시 null 허용 가능)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private PointTransactionType transactionType;

    @Column(nullable = false)
    private Long amount; // 변동 금액 (절대값으로 저장하고 타입으로 구분하는 것이 관리상 편리합니다)

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PointHistory(Member member, Payment payment, PointTransactionType transactionType, Long amount) {
        this.member = member;
        this.payment = payment;
        this.transactionType = transactionType;
        this.amount = amount;
    }
}
