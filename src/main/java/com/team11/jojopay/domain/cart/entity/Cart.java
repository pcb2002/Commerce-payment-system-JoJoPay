package com.team11.jojopay.domain.cart.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;



@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "carts")
public class Cart extends BaseTimeEntity {

    // 장바구니 pk
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 회원 참조
     * fetch = LAZY로 member를 조회할 때 사용
     * unique = true → 회원 1명당 1개의 장바구니 사용
     */

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "member_Id",
            nullable = false,
            unique = true
    )
    private Member member;


}
