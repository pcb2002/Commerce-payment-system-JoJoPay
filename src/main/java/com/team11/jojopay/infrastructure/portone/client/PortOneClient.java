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

  @Value("${portone.api-url}")
  private String apiUrl;

  /**
   * 포트원 V2 서버 엔진에 REST GET 웹 요청을 송출하여 해당 결제 건의 상세 거래 정보를 획득합니다.
   *
   * @param portonePaymentId 포트원 허브 채널사에서 고유하게 발급한 결제 원장 고유 식별 거래 코드 (TID)
   * @return 포트원 측 원본 결제 상태정보 및 금액 필드가 매핑된 PortOnePaymentResponse DTO 객체
   * @throws ServiceException 포트원 API 서버 장애 및 망 응답 타임아웃 지연 시 발생
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
   * 포트원 결제 취소 엔드포인트로 POST 원격 제어 신호를 송출하여 승인된 카드 금액을 강제 취소/환불 처리합니다.
   * 오버로딩 또는 신규 기능 명세에 맞춤에 따라, 세 번째 파라미터로 정밀 취소 금액(amount)을 필수로 전송하도록 구현을 확장했습니다.
   * 이 값을 지정하여 전송하면 포트원 V2 명세 규칙상 자동으로 '부분 환불' 체계로 인식되어 동작합니다.
   *
   * @param portonePaymentId 포트원 허브 채널사에서 발급한 고유 결제 식별 키 (TID)
   * @param reason           고객이 기입한 환불 사유 정보 문자열 (포트원 대시보드 표출용)
   * @param amount           이번 취소 회차에서 포트원 계좌/카드단에서 실제로 차감 취소시킬 실 정산금 금액
   * @throws ServiceException 포트원 내부 밸리데이션 한도 실패 혹은 네트워크 게이트웨이 파손 시 발생
   */
  public void cancelPayment(String portonePaymentId, String reason, Long amount) {
    String url = apiUrl + "/payments/" + portonePaymentId + "/cancel";

    HttpHeaders headers = createHeaders();

    Map<String, Object> body = new HashMap<>();
    body.put("reason", reason);
    body.put("amount", amount); // 포트원 API 서버에 "이 금액만큼 부분 취소해줘"라고 전달

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    try {
      restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.PAYMENT_CANCEL_FAILED);
    }
  }

  /**
   * 정기 구독 결제를 처리를 위해 유저의 카드 암호화 열쇠인 빌링키(BillingKey)를 기반으로 자동 출금 결제를 수행합니다.
   *
   * @param billingKey 유저의 개인 카드 정보가 안전하게 래핑된 포트원 측 암호화 토큰 키
   * @param paymentId    멱등성 유지를 위해 조합될 고유 주문 고유 식별 정보 키
   * @param amount     출금 결제 요청할 총 대금 원가액
   * @param orderName  카드 영수증 명세서에 표출될 상품 대분류 명칭 정보
   * @return 결제 승인 결과를 파싱한 PortOnePaymentResponse DTO 객체
   * @throws ServiceException 한도 초과, 분실 카드, 혹은 구독 연동 모듈 장애 시 발생
   */
  public PortOnePaymentResponse scheduleBillingKeyPayment(String billingKey, String paymentId, Long amount, String orderName) {
    String url = apiUrl + "/payments-by-billing-key";

    HttpHeaders headers = createHeaders();

    Map<String, Object> body = new HashMap<>();
    body.put("billingKey", billingKey);
    body.put("paymentId", paymentId);
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
   * 포트원 웹훅 커넥터 서버에서 날아온 알림 신호의 무결성을 해시 코드로 상호 대조하여 해커의 위조 패킷 공격을 탐지 차단합니다.
   *
   * @param webhookBody             포트원 원격 서버가 비동기로 전송한 오리지널 JSON Plain 텍스트 문자열 바디
   * @param receivedHeaderSignature 포트원 전송 패킷 HTTP 헤더인 'Authorization' 스펙 내 동봉된 해시 서명 문자열
   * @return 대조 연산 결과 디지털 봉인 인장이 한 자의 오차도 없이 일치하여 검증에 통과하면 true, 데이터 오염이나 공격 탐지 시 false
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
   * 포트원 V2 REST API 공식 가이드라인 규격 인증을 통과하기 위해 헤더에 보안 암호화 시크릿 토큰 인장을 결합하는 공통 유틸 메서드입니다.
   */
  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "PortOne " + secretKey);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
