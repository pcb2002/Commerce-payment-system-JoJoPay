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

    if (billingKeyRepository.existsByCustomerUid(request.getCustomerUid())) {
      throw new ServiceException(ErrorCode.BILLING_KEY_DUPLICATE);
    }

    BillingKey billingKey = BillingKey.create(
        member,
        request.getCustomerUid(),
        request.getCardName(),
        request.getCardNumber()
    );

    BillingKey savedBillingKey = billingKeyRepository.save(billingKey);

    return BillingKeyResponse.from(savedBillingKey);
  }

  @Transactional(readOnly = true)
  public List<BillingKeyResponse> getMyBillingKeys(Long memberId) {
    return billingKeyRepository.findAllByMemberIdAndStatus(memberId, BillingKeyStatus.ACTIVE)
        .stream()
        .map(BillingKeyResponse::from)
        .toList();
  }

  @Transactional
  public void deleteBillingKey(Long memberId, Long billingKeyId) {
    BillingKey billingKey = billingKeyRepository.findByIdAndMemberId(billingKeyId, memberId)
        .orElseThrow(() -> new ServiceException(ErrorCode.BILLING_KEY_NOT_FOUND));

    billingKey.delete();
  }
}
