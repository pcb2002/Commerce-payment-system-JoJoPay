package com.team11.jojopay.domain.auth.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.auth.dto.request.LoginRequest;
import com.team11.jojopay.domain.auth.dto.request.SignupRequest;
import com.team11.jojopay.domain.auth.dto.response.LoginResponse;
import com.team11.jojopay.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API를 처리하는 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  /**
   * 회원가입 요청을 처리
   */
  @PostMapping("/signup")
  public CommonApiResponse<Void> signup(
      @Valid @RequestBody SignupRequest request
  ) {
    authService.signup(request);

    return CommonApiResponse.success(CREATED, "회원가입 성공", null);
  }

  /**
   * 로그인 요청을 처리하고 JWT 토큰을 반환
   */
  @PostMapping("/login")
  public CommonApiResponse<LoginResponse> login(
      @Valid @RequestBody LoginRequest request
  ) {
    LoginResponse response = authService.login(request);

    return CommonApiResponse.success(OK, "로그인 성공", response);
  }
}
