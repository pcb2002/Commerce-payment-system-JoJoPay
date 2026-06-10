-- 1. MEMBER
INSERT INTO member (id, email, password, name, phone_number, point_balance, total_payment_amount, membership_grade, created_at, updated_at)
VALUES (1, 'user1@test.com', 'h1', '홍길동', '01011112222', 10000, 150000, 'NORMAL', NOW(), NOW());

INSERT INTO member (id, email, password, name, phone_number, point_balance, total_payment_amount, membership_grade, created_at, updated_at)
VALUES (2, 'sub@test.com', 'h2', '김구독', '01033334444', 50000, 200000, 'VVIP', NOW(), NOW());

-- 2. PRODUCT
INSERT INTO product (id, name, price, stock, description, status, category, created_at, updated_at)
VALUES (1, '게이밍 키보드', 150000, 50, '기계식 키보드', 'ON_SALE', 'ELECTRONICS', NOW(), NOW());
INSERT INTO product (id, name, price, stock, description, status, category, created_at, updated_at)
VALUES (2, '무선 마우스', 30000, 100, '고감도 마우스', 'ON_SALE', 'ELECTRONICS', NOW(), NOW());
INSERT INTO product (id, name, price, stock, description, status, category, created_at, updated_at)
VALUES (3, '친환경 텀블러', 15000, 30, '가벼운 텀블러', 'ON_SALE', 'ACCESSORY', NOW(), NOW());
INSERT INTO product (id, name, price, stock, description, status, category, created_at, updated_at)
VALUES (4, '대나무 헬리콥터', 1000, 30, '테스트용', 'ON_SALE', 'ACCESSORY', NOW(), NOW());

-- 3. CART
INSERT INTO cart (id, member_id, created_at, updated_at) VALUES (1, 1, NOW(), NOW());
INSERT INTO cart_item (id, cart_id, product_id, quantity, created_at, updated_at) VALUES (1, 1, 1, 1, NOW(), NOW());

-- 4. BILLING_KEY & SUBSCRIPTION
INSERT INTO billing_key (id, member_id, customer_uid, card_name, card_number, status, created_at, updated_at)
VALUES (1, 2, 'uid_kim_sub', '현대카드', '1234-****', 'ACTIVE', NOW(), NOW());

INSERT INTO subscription (id, member_id, billing_key_id, plan_name, price, status, next_billing_date, created_at, updated_at)
VALUES (1, 2, 1, 'PREMIUM', 9900, 'ACTIVE', DATEADD('DAY', 30, NOW()), NOW(), NOW());

-- 5. ORDERS & PAYMENT (H2 전용 DATEADD 사용)
INSERT INTO orders (order_id, order_number, member_id, status, total_amount, used_point, created_at, updated_at)
VALUES (1, 'ORD-20260605-001', 1, 'COMPLETED', 150000, 0, DATEADD('DAY', -4, NOW()), DATEADD('DAY', -4, NOW()));

INSERT INTO order_item (order_item_id, order_id, product_id, product_name, price_at_order, quantity, status, created_at, updated_at)
VALUES (1, 1, 1, '게이밍 키보드', 150000, 1, 'COMPLETED', DATEADD('DAY', -4, NOW()), DATEADD('DAY', -4, NOW()));

INSERT INTO payment (id, order_id, member_id, portone_payment_id, amount, pg_real_amount, used_point, status, pg_provider, created_at, updated_at)
VALUES (1, 1, 1, 'pay_uuid_123', 150000, 150000, 0, 'COMPLETED', 'KAKAO', DATEADD('DAY', -4, NOW()), DATEADD('DAY', -4, NOW()));

-- 6. POINT_HISTORY
INSERT INTO point_history (id, member_id, payment_id, transaction_type, amount, created_at)
VALUES (1, 1, 1, 'EARN', 500, NOW());