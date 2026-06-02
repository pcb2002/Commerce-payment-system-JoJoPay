package com.team11.jojopay.infrastructure.portone.client;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class PortOneClient {

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${portone.secret-key}")
  private String secretKey;

  @Value("{portone.api-url}")
  private String apiUrl;

  /**
   * 포트원 V2 API를 통해 결제 상세 정보를 조회합니다.
   */
  public PortOnePaymentResponse getPaymentInfo(String portonePaymentId) {
    String url = apiUrl + "/payments/" + portonePaymentId;

    // 헤더 설정
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "PortOne " + secretKey);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      return restTemplate.exchange(url, HttpMethod.GET, entity, PortOnePaymentResponse.class).getBody();
    } catch (Exception e) {
      // 통신 실패 시 PG 에러 코드 활용
      throw new ServiceException(ErrorCode.PAYMENT_GATEWAY_ERROR);
    }
  }
}
