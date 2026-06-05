package com.team11.jojopay.domain.subscription.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.subscription.dto.request.BillingKeyRegisterRequest;
import com.team11.jojopay.domain.subscription.dto.response.BillingKeyResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.enums.BillingKeyStatus;
import com.team11.jojopay.domain.subscription.repository.BillingKeyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 빌링키 등록, 조회, 삭제 등 결제수단 관련 비즈니스 로직을 처리하는 Service
 */
@Service
@RequiredArgsConstructor
public class BillingKeyService {

  private final BillingKeyRepository billingKeyRepository;

  private final MemberRepository memberRepository;

  /**
   * 로그인한 회원의 결제수단을 등록
   *
   * @param memberId 로그인한 회원 ID
   * @param request 등록할 빌링키 정보
   * @return 등록된 빌링키 응답 정보
   */
  @Transactional
  public BillingKeyResponse registerBillingKey(
      Long memberId,
      BillingKeyRegisterRequest request
  ) {

    Member member = memberRepository.findById(memberId).orElseThrow(
        () -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND)
    );

    // customerUid 중복 등록 방지
    if (billingKeyRepository.existsByCustomerUid(request.getCustomerUid())) {
      throw new ServiceException(ErrorCode.BILLING_KEY_DUPLICATE);
    }

    // 결제수단은 최초 등록 시 ACTIVE 상태로 생성
    BillingKey billingKey = BillingKey.create(
        member,
        request.getCustomerUid(),
        request.getCardName(),
        request.getCardNumber()
    );

    BillingKey savedBillingKey = billingKeyRepository.save(billingKey);

    return BillingKeyResponse.from(savedBillingKey);
  }

  /**
   * 로그인한 회원의 활성 결제수단 목록을 조회
   */
  @Transactional(readOnly = true)
  public List<BillingKeyResponse> getMyBillingKeys(Long memberId) {
    return billingKeyRepository.findAllByMemberIdAndStatus(memberId, BillingKeyStatus.ACTIVE)
        .stream()
        .map(BillingKeyResponse::from)
        .toList();
  }

  /**
   * 로그인한 회원의 결제수단을 삭제 처리
   * 실제 데이터는 삭제하지 않고 DELETED 상태로 변경
   */
  @Transactional
  public void deleteBillingKey(Long memberId, Long billingKeyId) {
    BillingKey billingKey = billingKeyRepository.findByIdAndMemberId(billingKeyId, memberId)
        .orElseThrow(() -> new ServiceException(ErrorCode.BILLING_KEY_NOT_FOUND));

    billingKey.delete();
  }
}
