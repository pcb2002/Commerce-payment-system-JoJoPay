package com.team11.jojopay.domain.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.auth.controller.AuthController;
import com.team11.jojopay.domain.auth.dto.request.LoginRequest;
import com.team11.jojopay.domain.auth.dto.request.SignupRequest;
import com.team11.jojopay.domain.auth.dto.response.LoginResponse;
import com.team11.jojopay.domain.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // 🟢 시큐리티 간섭 완전 차단
@MockitoBean(types = JpaMetamodelMappingContext.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser
    @DisplayName("[POST] 회원가입 API 성공: 형식에 맞는 페이로드 전송 시 201 Created 가 반환된다.")
    void signup_Api_Success() throws Exception {
        // given
        String json = "{\"email\":\"test@test.com\",\"password\":\"password123\",\"name\":\"홍길동\",\"phoneNumber\":\"010-1234-5678\"}";
        SignupRequest request = objectMapper.readValue(json, SignupRequest.class);
        doNothing().when(authService).signup(any(SignupRequest.class));

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입 성공"));

        verify(authService, times(1)).signup(any(SignupRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("[POST] 회원가입 API 실패: 정규식 규칙(휴대폰 번호 형식 오류) 위반 시 400 Bad Request가 발생한다.")
    void signup_Api_Fail_Validation() throws Exception {
        // given: 하이픈이 없고 자릿수가 틀린 잘못된 휴대폰 번호 상신
        String json = "{\"email\":\"test@test.com\",\"password\":\"pass\",\"name\":\"홍길동\",\"phoneNumber\":\"010111\"}";
        SignupRequest invalidRequest = objectMapper.readValue(json, SignupRequest.class);

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).signup(any());
    }

    @Test
    @WithMockUser
    @DisplayName("[POST] 로그인 API 성공: 발급된 토큰 객체가 공통 응답 규격에 정상 직렬화된다.")
    void login_Api_Success() throws Exception {
        // given
        String json = "{\"email\":\"test@test.com\",\"password\":\"password123\"}";
        LoginRequest request = objectMapper.readValue(json, LoginRequest.class);
        LoginResponse response = LoginResponse.of("dummy_jwt_token_string");

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.jwtToken").value("dummy_jwt_token_string"));
    }
}
