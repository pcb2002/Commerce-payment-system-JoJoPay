package com.team11.jojopay.domain.point.controller;

import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.point.dto.response.PointBalanceResponse;
import com.team11.jojopay.domain.point.dto.response.PointHistoryResponse;
import com.team11.jojopay.domain.point.entity.PointHistory;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PointController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PointService pointService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser
    @DisplayName("[GET] 포인트 잔액 조회 성공")
    void getBalance() throws Exception {

        PointBalanceResponse response = new PointBalanceResponse(10000L);

        given(pointService.getBalance(any()))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/points/balance"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("포인트 잔액 조회 성공"))
                .andExpect(jsonPath("$.data.pointBalance").value(10000));
    }

    @Test
    @WithMockUser
    @DisplayName("[GET] 포인트 거래 내역 조회 성공")
    void getHistories() throws Exception {

        PointHistory history = Mockito.mock(PointHistory.class);

        given(history.getId()).willReturn(1L);
        given(history.getTransactionType()).willReturn(PointTransactionType.EARN);
        given(history.getAmount()).willReturn(5000L);
        given(history.getCreatedAt()).willReturn(LocalDateTime.of(2025, 1, 1, 12, 0));

        PointHistoryResponse response = new PointHistoryResponse(history);

        given(pointService.getHistories(any()))
                .willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/points/histories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("포인트 거래 내역 전체 최신순으로 조회 성공"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].transactionType").value("EARN"))
                .andExpect(jsonPath("$.data[0].amount").value(5000));
    }
}