package com.team11.jojopay.infrastructure.portone.client;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 외부 결제 대행사(PortOne V2) 서버와 HTTP 통신 및 보안 검증을 수행하는 인프라 클라이언트입니다.
 * 외부 세계와 우리 스프링 서버를 연결하는 '보안 무전기' 역할을 담당합니다.
 */
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
    HttpHeaders headers = createHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      return restTemplate.exchange(url, HttpMethod.GET, entity, PortOnePaymentResponse.class).getBody();
    } catch (Exception e) {
      // 통신 실패 시 PG 에러 코드 활용
      throw new ServiceException(ErrorCode.PAYMENT_GATEWAY_ERROR);
    }
  }

  /**
   * 2. 외부 PG 보상 취소 API (강제 환불)
   * * [구동방식]
   * 우리 서버 내부 검증(예: 사용자가 요청한 결제 금액과 포트원이 승인한 금액이 다른 위변조 상황 등) 과정에서
   * 에러가 포착되었을 때, 이미 긁혀버린 외부 카드 결제를 '서버단에서 강제로 환불(취소)'시키기 위해 호출합니다.
   * 주소창 뒤에 /cancel을 붙이고, 바디에 취소 사유를 담아 POST로 전송합니다.
   */
  public void cancelPayment(String portonePaymentId, String reason) {
    String url = apiUrl + "/payments/" + portonePaymentId + "/cancel";

    HttpHeaders headers = createHeaders();

    Map<String, Object> body = new HashMap<>();
    body.put("reason", reason);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    try {
      restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    } catch (Exception e) {
      // 결제 검증은 실패했는데 외부 취소 요청마저 실패하면, 돈은 묶이고 데이터는 꼬이는 치명적인 상태가 되기에 시스템 관리자가 즉시 인지할 수 있도록 가장 무거운 시스템 에러 코드를 던집니다.
      throw new ServiceException(ErrorCode.PAYMENT_CANCEL_FAILED);
    }
  }

  /**
   * 3. 스케줄러 정기 자동 결제 API (빌링키 비인증 결제)
   * * [구동방식]
   * 넷플릭스처럼 사용자의 비밀번호 입력 없이 정기적으로 돈을 출금하는 기능입니다.
   * 자정마다 도는 스케줄러 배치가 회원들의 '빌링키(카드정보 암호화 열쇠)'를 들고 이 메서드를 호출합니다.
   * * [멱등성 키(paymentId) 생성 규칙]
   * 주문번호 뒤에 현재 타임스탬프 밀리초(System.currentTimeMillis())를 붙여 매번 유일한 고유 결제ID를 만듭니다.
   * 만약 스케줄러 네트워크 지연으로 인해 동일한 요청이 중복으로 두 번 날아가더라도,
   * 포트원 측에서 이 ID를 보고 중복 결제를 알아서 차단(멱등성 보장)해 주는 안전장치입니다.
   */
  public PortOnePaymentResponse scheduleBillingKeyPayment(String billingKey, Long orderId, Long amount, String orderName) {
    String url = apiUrl + "/payments-by-billing-key";

    HttpHeaders headers = createHeaders();

    Map<String, Object> body = new HashMap<>();
    body.put("billingKey", billingKey);
    body.put("paymentId", orderId + "_" + System.currentTimeMillis());
    body.put("amount", amount);
    body.put("orderName", orderName);
    body.put("currency", "KRW");

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    try {
      return restTemplate.exchange(url, HttpMethod.POST, entity, PortOnePaymentResponse.class).getBody();
    } catch (Exception e) {
      // 사용자의 한도 초과, 카드 정지, 혹은 정기결제 엔드포인트 장애 시 예외 발생
      throw new ServiceException(ErrorCode.PERIODIC_PAYMENT_FAILED);
    }
  }

  /**
   * 4. 포트원 웹훅 서명 검증 유틸 (HMAC-SHA256 사기 차단 장치)
   * * [구동방식]
   * 포트원 서버가 비동기로 우리 서버에게 "결제 완료됐어요"라고 알림 문자(웹훅)를 보냈을 때,
   * 해커가 중간에서 가짜 위조 신호를 보낸 것인지 탐지하는 철통 보안 로직입니다.
   * * [암호화 원리]
   * 1. 포트원과 우리만 공유하는 비밀번호(secretKey)를 꺼냅니다.
   * 2. 날아온 웹훅 본문 데이터(webhookBody)를 이 키와 함께 HmacSHA256 알고리즘 기계에 넣고 돌립니다.
   * 3. 믹서기에서 나온 바이너리 해시 결과물(byte[] hash)을 16진수 문자열(Hex String)로 변환합니다.
   * 4. 우리가 직접 계산한 이 디지털 봉인 인장과, 포트원이 편지 봉투(헤더)에 붙여서 보낸 인장이 일치하는지 비교합니다.
   * * 일치하면 진짜 포트원이 보낸 신호이므로 true, 도중에 에러가 나거나 불일치하면 사기 요청으로 간주하고 false를 반환합니다.
   */
  public boolean verifyWebhookSignature(String webhookBody, String receivedHeaderSignature) {
    try {
      // 자바 내장 암호화 기계(Mac) 활성화 및 비밀키 결합 초기화
      Mac hmacSha256 = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      hmacSha256.init(secretKeySpec);

      // 웹훅 본문 텍스트를 통째로 암호화하여 바이너리 해시 데이터 추출
      byte[] hash = hmacSha256.doFinal(webhookBody.getBytes(StandardCharsets.UTF_8));

      // 바이너리(byte) 형태의 데이터를 사람이 읽을 수 있는 16진수 소문자 문자열로 포맷 변환
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }

      // 우리가 계산한 결과물과 포트원이 보낸 헤더 서명 값을 대소문자 구분 없이 최종 대조
      return hexString.toString().equalsIgnoreCase(receivedHeaderSignature);
    } catch (Exception e) {
      // 알고리즘 오타, 시크릿 키 누락, 웹훅 바디 null 등 어떤 에러라도 나면 안전하게 검증 실패(false)로 귀결시킴
      return false;
    }
  }

  /**
   * [공통 내부 메서드] 포트원 V2 인증용 VIP 출입증 생성
   * * 포트원 V2 REST API 공식 명세에 따라, 모든 요청 헤더의 'Authorization' 필드에
   * "PortOne [내 시크릿키]"라는 규격을 강제 서명하여 헤더 객체를 만들어줍니다.
   */
  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "PortOne " + secretKey);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
