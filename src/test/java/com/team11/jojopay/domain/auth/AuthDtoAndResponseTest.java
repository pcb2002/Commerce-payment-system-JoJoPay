package com.team11.jojopay.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.team11.jojopay.domain.auth.dto.request.LoginRequest;
import com.team11.jojopay.domain.auth.dto.request.SignupRequest;
import com.team11.jojopay.domain.auth.dto.response.LoginResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AuthDtoAndResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("SignupRequest 원천 데이터 바인딩 맵퍼 작동 스펙 검증")
    void signupRequest_Fields_Test() throws Exception {
        // given
        String json = "{\"email\":\"model@test.com\",\"password\":\"secure1234\",\"name\":\"임꺽정\",\"phoneNumber\":\"010-9999-8888\"}";

        // when
        SignupRequest request = objectMapper.readValue(json, SignupRequest.class);

        // then
        assertThat(request.getEmail()).isEqualTo("model@test.com");
        assertThat(request.getPassword()).isEqualTo("secure1234");
        assertThat(request.getName()).isEqualTo("임꺽정");
        assertThat(request.getPhoneNumber()).isEqualTo("010-9999-8888");
    }

    @Test
    @DisplayName("LoginRequest 원천 데이터 바인딩 맵퍼 작동 스펙 검증")
    void loginRequest_Fields_Test() throws Exception {
        // given
        String json = "{\"email\":\"login@test.com\",\"password\":\"pass1234\"}";

        // when
        LoginRequest request = objectMapper.readValue(json, LoginRequest.class);

        // then
        assertThat(request.getEmail()).isEqualTo("login@test.com");
        assertThat(request.getPassword()).isEqualTo("pass1234");
    }

    @Test
    @DisplayName("LoginResponse 정적 팩토리 메서드 및 가용 필드 일치성 검증")
    void loginResponse_Static_Factory_Test() {
        // given & when
        LoginResponse response = LoginResponse.of("jojopay_verified_secret_jwt_token");

        // then
        assertThat(response.getJwtToken()).isEqualTo("jojopay_verified_secret_jwt_token");
    }
}
