package com.team11.jojopay.domain.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.team11.jojopay.domain.point.dto.response.PointBalanceResponse;
import com.team11.jojopay.domain.point.dto.response.PointHistoryResponse;
import com.team11.jojopay.domain.point.entity.PointHistory;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PointResponseTest {

    @Test
    @DisplayName("PointBalanceResponse 모델 생성 및 필드 데이터 일치성 검증")
    void pointBalanceResponse_Test() {
        // given & when
        PointBalanceResponse balanceResponse = new PointBalanceResponse(35000L);

        // then
        assertThat(balanceResponse.getPointBalance()).isEqualTo(35000L);
    }

    @Test
    @DisplayName("PointHistoryResponse 컨버터 팩토리 검증: 엔티티 가짜 객체 규격이 DTO 응답 명세서로 오차 없이 이식된다.")
    void pointHistoryResponse_From_Entity_Test() {
        // given: 순수 Mockito 기술로 가짜 이력 데이터 조립
        PointHistory mockHistory = mock(PointHistory.class);
        LocalDateTime now = LocalDateTime.now();

        given(mockHistory.getId()).willReturn(77L);
        given(mockHistory.getTransactionType()).willReturn(PointTransactionType.USE);
        given(mockHistory.getAmount()).willReturn(2500L);
        given(mockHistory.getCreatedAt()).willReturn(now);

        // when: 생성자 주입 변환 구동
        PointHistoryResponse historyResponse = new PointHistoryResponse(mockHistory);

        // then
        assertThat(historyResponse.getId()).isEqualTo(77L);
        assertThat(historyResponse.getTransactionType()).isEqualTo(PointTransactionType.USE);
        assertThat(historyResponse.getAmount()).isEqualTo(2500L);
        assertThat(historyResponse.getCreatedAt()).isEqualTo(now);
    }
}