package com.team11.jojopay.domain.auth.dto.response;

import lombok.Getter;

@Getter
public class LoginResponse {

  private final String jwtToken;

  private LoginResponse(String jwtToken) {
    this.jwtToken = jwtToken;
  }

  public static LoginResponse of(String jwtToken) {
    return new LoginResponse(jwtToken);
  }
}