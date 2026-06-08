package com.team11.jojopay.domain.point.service;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.point.dto.response.PointHistoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.sql.init.mode=never")  // 처음 시작할때 data.sql과 schema.sql의 테이블 존재로 인한 충돌 때문에 조건 추가
class PointServiceTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("동시에 100개의 포인트 충전 요청이 들어와도 비관적 락을 통해 잔액이 정확히 합산되어야 한다.")
    void chargePointConcurrencyTest() throws InterruptedException {
        // given: 초기 잔액이 0원인 회원 생성 및 저장
        Member member = Member.signup(
                "홍길동",
                "test@test.com",
                "passwordHash123",
                "010-1234-5678"
        );
        // DB에 저장하면 ID(Identitiy)가 할당되고, 기본 잔액 0원이 세팅됩니다.
        Member savedMember = memberRepository.save(member);

        int threadCount = 100;
        Long chargeAmount = 1000L;

        // 멀티스레드 구동을 위한 인프라 세팅
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 100개의 스레드가 동시에 1,000원씩 충전 요청 유도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargeMockPoint(savedMember.getId(), chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드의 작업이 끝날 때까지 대기

        // then: 일반 조회였다면 금액이 유실되지만, 자물쇠(락) 덕분에 100,000원이 정확히 정산되어야 함
        Member finalMember = memberRepository.findById(savedMember.getId()).orElseThrow();
        assertThat(finalMember.getPointBalance()).isEqualTo(threadCount * chargeAmount);

        // 이력 테이블에도 정확히 100건의 충전 기록이 쌓였는지 확인
        List<PointHistoryResponse> histories = pointService.getHistories(savedMember.getId());
        assertThat(histories).hasSize(threadCount);
    }
}