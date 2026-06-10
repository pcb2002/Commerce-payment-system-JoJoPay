package com.team11.jojopay.domain.point.repository;

import com.team11.jojopay.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointRepository extends JpaRepository<PointHistory, Long> {

    // 특정 회원의 포인트 거래 내역을 최신순(생성일 내림차순)으로 전체 조회
    List<PointHistory> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

}
