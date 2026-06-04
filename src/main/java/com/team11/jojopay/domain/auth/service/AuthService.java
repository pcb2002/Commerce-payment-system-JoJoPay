package com.team11.jojopay.domain.auth.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.auth.dto.request.LoginRequest;
import com.team11.jojopay.domain.auth.dto.request.SignupRequest;
import com.team11.jojopay.domain.auth.dto.response.LoginResponse;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;

  /**
   * 회원가입 처리
   * 이메일 중복 확인 후 비밀번호를 암호화하여 회원을 저장
   */
  @Transactional
  public void signup(SignupRequest request) {
    if (memberRepository.existsByEmail(request.getEmail())) {
      throw new ServiceException(ErrorCode.EMAIL_DUPLICATE);
    }

    String encodedPassword = passwordEncoder.encode(request.getPassword());

    Member member = Member.signup(
        request.getName(),
        request.getEmail(),
        encodedPassword,
        request.getPhoneNumber()
    );

    memberRepository.save(member);
  }

  /**
   * 로그인 처리
   * 이메일과 비밀번호를 검증한 뒤 JWT 토큰을 발급
   */
  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    Member member = memberRepository.findByEmail(request.getEmail()).orElseThrow(
        () -> new ServiceException(ErrorCode.INVALID_CREDENTIALS)
    );

    if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
      throw new ServiceException(ErrorCode.INVALID_CREDENTIALS);
    }

    String jwtToken = jwtProvider.createAccessToken(member);

    return LoginResponse.of(jwtToken);
  }
}