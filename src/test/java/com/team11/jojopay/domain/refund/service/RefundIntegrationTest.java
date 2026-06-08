package com.team11.jojopay.domain.refund.service;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.entity.OrderItem;
import com.team11.jojopay.domain.order.enums.OrderStatus;
import com.team11.jojopay.domain.order.enums.OrderItemStatus;
import com.team11.jojopay.domain.order.reopsitory.OrderRepository;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.Category;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import com.team11.jojopay.domain.refund.dto.request.RefundRequest;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@Transactional
public class RefundIntegrationTest {

    @Autowired private RefundService refundService;
    @Autowired private PointService pointService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private EntityManager em;

    @MockitoBean private PortOneClient portOneClient;

    private Member testMember;
    private Order orderA;
    private OrderItem orderItemA;

    @BeforeEach
    void setUp() throws Exception {
        // 1. 테스트 유저 생성 및 저장
        testMember = Member.signup("홍길동", "hong@test.com", "password", "01012345678");
        ReflectionTestUtils.setField(testMember, "pointBalance", 0L);
        memberRepository.save(testMember);

        // 2. Product 생성 (정적 팩토리 메서드나 빌더가 존재한다면 규칙에 맞게 우회 최소화 권장)
        // 여기서는 리플렉션을 유지하되 가독성 있게 정리
        Constructor<Product> constructor = Product.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Product product = constructor.newInstance();
        ReflectionTestUtils.setField(product, "name", "더비슈즈");
        ReflectionTestUtils.setField(product, "price", 1000000L);
        ReflectionTestUtils.setField(product, "stock", 50);
        ReflectionTestUtils.setField(product, "description", "소가죽 더비슈즈");
        ReflectionTestUtils.setField(product, "status", ProductStatus.ON_SALE);
        ReflectionTestUtils.setField(product, "category", Category.SHOES);
        productRepository.save(product);

        // 3. 실제 엔티티 스펙에 맞춘 OrderItem 생성 (최초 상태: COMPLETED)
        orderItemA = OrderItem.createOrderItem(product.getId(), product.getName(), product.getPrice(), 1);

        // 4. 실제 엔티티 스펙에 맞춘 Order 생성 및 연관관계 매핑 (최초 상태: COMPLETED)
        orderA = Order.createOrder(testMember.getId(), "ORD-A-2026", 1000000L, 0L);
        orderA.addOrderItem(orderItemA);
        orderA.completeOrder(); // 주문 완료 처리
        orderRepository.save(orderA);

        // 5. 결제 원장 고정
        Payment payment = Payment.createPayment(
                orderA,
                testMember.getId(),
                "portone_mock_id_1234",
                1000000L,
                0L
        );
        payment.complete();
        paymentRepository.save(payment);

        // 6. [포인트 흐름 시뮬레이션] 10,000포인트 적립 후 소진 상황 유도
        pointService.chargeMockPoint(testMember.getId(), 10000L);
        ReflectionTestUtils.setField(testMember, "pointBalance", 0L);
        memberRepository.save(testMember);
    }

    private RefundRequest createRefundRequest(String orderNumber, Long orderItemId, Integer quantity) {
        RefundRequest request = new RefundRequest();
        ReflectionTestUtils.setField(request, "orderNumber", orderNumber);
        ReflectionTestUtils.setField(request, "reason", "포인트 상계 회수 테스트");

        RefundRequest.RefundItemRequest itemRequest = new RefundRequest.RefundItemRequest();
        ReflectionTestUtils.setField(itemRequest, "orderItemId", orderItemId);
        ReflectionTestUtils.setField(itemRequest, "quantity", quantity);

        ReflectionTestUtils.setField(request, "items", List.of(itemRequest));
        return request;
    }

    @Test
    @DisplayName("🟢 [환불 성공 - 포인트 상계] 적립 포인트를 이미 소진한 후 환불 시, 부족한 포인트만큼 PG 환급액에서 차감된다.")
    void refund_DeductFromPgAmount_Success() {
        // given
        RefundRequest request = createRefundRequest(orderA.getOrderNumber(), orderItemA.getId(), 1);

        // when
        refundService.refundOrder(testMember.getId(), request);

        // 🎯 중요: 트랜잭션 2 결과가 영속성 캐시에 머무르지 않고 DB 상태를 강제 갱신해 오도록 동기화
        em.flush();
        em.clear();

        // then
        // 1. [포인트 잔액 검증]
        Member updatedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(updatedMember.getPointBalance()).isEqualTo(0L);

        // 2. [PG 환급액 검증]
        verify(portOneClient).cancelPayment(
                anyString(),
                Mockito.eq("포인트 상계 회수 테스트"),
                Mockito.eq(990000L)
        );

        // 3. 🎯 [교정된 도메인 상태 분리 검증] 하위 상품 상태와 상위 주문 원장의 상태 전파 검증

        // 하위 주문 상품: 1개 산 내역이 전부 환불 처리를 탔으므로 REFUNDED 완료 상태여야 함
        OrderItem updatedItem = em.find(OrderItem.class, orderItemA.getId());
        assertThat(updatedItem.getStatus()).isEqualTo(OrderItemStatus.REFUNDED);

        // 상위 주문 원장: 하위 모든 상품 품목이 다 REFUNDED 되었으므로 FULLY_REFUNDED 처리 완료여야 함
        Order updatedOrder = orderRepository.findById(orderA.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.FULLY_REFUNDED);
    }
}