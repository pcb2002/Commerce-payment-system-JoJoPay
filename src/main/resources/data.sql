-- ==========================================
-- 1. 회원(Member) 데이터
-- ==========================================
-- 일반 유저 (포인트 5000원 보유, 구독 미가입 상태 테스트용)
INSERT INTO member (member_id, email, name, role, point_balance, created_at, updated_at)
VALUES (1, 'user1@test.com', '김테스트', 'USER', 5000, NOW(), NOW());

-- 구독 가입 유저 (포인트 결제 및 정기 결제 테스트용)
INSERT INTO member (member_id, email, name, role, point_balance, created_at, updated_at)
VALUES (2, 'user2@test.com', '이구독', 'USER', 15000, NOW(), NOW());


-- ==========================================
-- 2. 상품(Product) 데이터
-- ==========================================
-- 재고가 넉넉한 일반 상품
INSERT INTO product (product_id, name, price, stock, status, created_at, updated_at)
VALUES (1, '개발자용 무접점 키보드', 150000, 100, 'ON_SALE', NOW(), NOW());

-- 동시성 테스트용 상품 (재고 적음)
INSERT INTO product (product_id, name, price, stock, status, created_at, updated_at)
VALUES (2, '선착순 한정판 마우스', 50000, 5, 'ON_SALE', NOW(), NOW());

-- 품절 상품 (예외 발생 테스트용)
INSERT INTO product (product_id, name, price, stock, status, created_at, updated_at)
VALUES (3, '인기 텀블러(품절)', 20000, 0, 'OUT_OF_STOCK', NOW(), NOW());


-- ==========================================
-- 3. 장바구니(Cart) 및 장바구니 아이템(CartItem) 데이터
-- ==========================================
-- 회원 1의 장바구니 생성 (MEMBER ||--|| CART)
INSERT INTO cart (cart_id, member_id, created_at, updated_at)
VALUES (1, 1, NOW(), NOW());

-- 회원 1의 장바구니에 키보드 1개, 한정판 마우스 2개 담기 (CART ||--o{ CART_ITEM)
INSERT INTO cart_item (cart_item_id, cart_id, product_id, quantity, created_at, updated_at)
VALUES (1, 1, 1, 1, NOW(), NOW());
INSERT INTO cart_item (cart_item_id, cart_id, product_id, quantity, created_at, updated_at)
VALUES (2, 1, 2, 2, NOW(), NOW());


-- ==========================================
-- 4. 포인트 이력(Point_History) 데이터
-- ==========================================
-- MEMBER ||--o{ POINT_HISTORY
-- 회원 1의 초기 가입 축하 포인트 지급 이력
INSERT INTO point_history (point_history_id, member_id, amount, type, description, created_at)
VALUES (1, 1, 5000, 'EARN', '회원가입 축하 포인트', NOW());

-- 회원 2의 포인트 누적 이력
INSERT INTO point_history (point_history_id, member_id, amount, type, description, created_at)
VALUES (2, 2, 15000, 'EARN', '이벤트 참여 보상', NOW());


-- ==========================================
-- 5. 주문(Orders) 및 결제(Payment) 초기 데이터 (선택 사항)
-- 이미 결제가 완료된 과거 내역 조회(목록/상세) 테스트용
-- ==========================================
-- 회원 2의 과거 주문 내역 1건 (MEMBER ||--o{ ORDERS)
INSERT INTO orders (order_number, member_id, status, total_amount, point_used, pg_amount, created_at, updated_at)
VALUES ('ORD-20260605-0001', 2, 'PAYMENT_COMPLETED', 150000, 0, 150000, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 해당 주문의 상품 스냅샷 (주문 당시 상품명과 가격 저장)
INSERT INTO order_item (order_item_id, order_number, product_id, product_name, price_at_order, quantity, created_at)
VALUES (1, 'ORD-20260605-0001', 1, '개발자용 무접점 키보드', 150000, 1, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 해당 주문의 결제 내역
INSERT INTO payment (payment_id, order_number, portone_payment_id, status, amount, method, created_at)
VALUES (1, 'ORD-20260605-0001', 'imp_1234567890', 'PAID', 150000, 'CARD', DATE_SUB(NOW(), INTERVAL 1 DAY));


-- ==========================================
-- 6. 구독(Subscription) 및 빌링키(Billing_Key) 데이터 (도전 과제용)
-- ==========================================
-- 회원 2가 등록한 카드 빌링키 (MEMBER ||--o{ BILLING_KEY)
INSERT INTO billing_key (billing_key_id, member_id, portone_billing_key, card_company, is_active, created_at, updated_at)
VALUES (1, 2, 'bln_abcefg_123', '현대카드', true, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 회원 2의 활성화된 정기 구독 신청 정보 (MEMBER ||--o{ SUBSCRIPTION)
-- 다음 결제일(next_payment_date)을 오늘 날짜로 세팅하여 스케줄러 테스트 시 바로 실행되도록 함
INSERT INTO subscription (subscription_id, member_id, billing_key_id, plan_name, status, next_payment_date, created_at, updated_at)
VALUES (1, 2, 1, '프리미엄 멤버십', 'ACTIVE', CURRENT_DATE(), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));