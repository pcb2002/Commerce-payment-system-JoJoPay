package com.team11.jojopay.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignupRequest {

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
  private String password;

  @NotBlank(message = "이름은 필수입니다.")
  private String name;

  @NotBlank(message = "휴대폰 번호는 필수입니다.")
  @Pattern(
      regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$",
      message = "올바른 휴대폰 번호 형식이 아닙니다."
  )
  private String phoneNumber;
}