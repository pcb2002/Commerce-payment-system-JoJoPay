package com.team11.jojopay.domain.refund.repository;

import com.team11.jojopay.domain.refund.entity.Refund;
import com.team11.jojopay.domain.refund.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Refund(환불 마스터 마이그레이션 데이터 영수증) 엔티티에 대한 영속성 데이터 액세스를 전담하는 레포지토리 인터페이스입니다.
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * 특정 상태(Status)를 유지한 채 일정 시간이 경과하여 낙오된 환불 원장 목록을 전수 조회합니다.
     * [주요 활용처]
     * 내부 DB 비즈니스 처리(재고 복구, 포인트 가감산) 및 READY 상태 선저장은 정상 완료되었으나,
     * 외부 PG사(PortOne) API 호출 직전 단계에서 시스템 다운/정전이 발생했거나 
     * 네트워크 타임아웃 등으로 인해 사후 확정(COMPLETED/FAILED) 처리를 짓지 못한 
     * '유령/낙오 데이터(Stranded Data)'를 찾아내는 자동화 스케줄러(Batch)의 원천 데이터 추출용으로 사용됩니다.
     *
     * @param refundStatus 대상 환불 데이터의 현재 추적 상태 (ex: RefundStatus.READY)
     * @param threshold    조회 기준점이 되는 임계 시간축 (ex: 현재 시간 기준 30분 전으로 설정하여 실시간 처리 중인 건과의 간섭을 방지)
     * @return 임계 시간보다 이전에 생성되어 방치된 환불 원장(Refund) 엔티티 리스트
     */
    List<Refund> findAllByStatusAndCreatedAtBefore(RefundStatus refundStatus, LocalDateTime threshold);

    @Query("SELECT r FROM Refund r JOIN r.payment p JOIN p.order o WHERE o.memberId = :memberId ORDER BY r.createdAt DESC")
    List<Refund> findAllByMemberId(@Param("memberId") Long memberId);
}
