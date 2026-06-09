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
@Table(name = "point_history")
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
    private Long amount;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PointHistory(Member member, Payment payment, PointTransactionType transactionType, Long amount) {
        this.member = member;
        this.payment = payment;
        this.transactionType = transactionType;
        this.amount = determineAmountByTransactionType(transactionType, amount);
    }

    /**
     * 트랜잭션 타입에 따라 변동 금액의 부호를 결정합니다.
     * USE(사용)이거나 CANCEL(취소/회수) 등 차감 성격의 타입일 때 마이너스 처리합니다.
     */
    private Long determineAmountByTransactionType(PointTransactionType type, Long amount) {
        // 음수 입력을 방지하기 위해 먼저 절대값 처리 후 분기
        Long absoluteAmount = Math.abs(amount);

        // 💡 팀에서 정의한 Enum 구조에 맞게 차감 유형들을 케이스로 묶어줍니다.
        if (type == PointTransactionType.EARN || type == PointTransactionType.USE_RECOVERY) {
            return absoluteAmount;
        }

        if (type == PointTransactionType.USE || type == PointTransactionType.EARN_FORFEIT) {
            return -absoluteAmount;
        }

        return absoluteAmount;
    }
}
