-- 1. 회원 (Member)
CREATE TABLE member (
                        member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        email VARCHAR(255) NOT NULL UNIQUE,
                        name VARCHAR(100) NOT NULL,
                        role VARCHAR(50) NOT NULL,
                        point_balance INT DEFAULT 0,
                        created_at DATETIME NOT NULL,
                        updated_at DATETIME NOT NULL
);

-- 2. 상품 (Product)
CREATE TABLE product (
                         product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         price INT NOT NULL,
                         stock INT NOT NULL,
                         status VARCHAR(50) NOT NULL, -- ON_SALE, OUT_OF_STOCK 등
                         created_at DATETIME NOT NULL,
                         updated_at DATETIME NOT NULL
);

-- 3. 장바구니 (Cart)
-- 회원 1명당 1개의 장바구니 (1:1 관계)
CREATE TABLE cart (
                      cart_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      member_id BIGINT NOT NULL UNIQUE,
                      created_at DATETIME NOT NULL,
                      updated_at DATETIME NOT NULL,
                      FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- 4. 장바구니 아이템 (Cart_Item)
CREATE TABLE cart_item (
                           cart_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           cart_id BIGINT NOT NULL,
                           product_id BIGINT NOT NULL,
                           quantity INT NOT NULL,
                           created_at DATETIME NOT NULL,
                           updated_at DATETIME NOT NULL,
                           FOREIGN KEY (cart_id) REFERENCES cart(cart_id),
                           FOREIGN KEY (product_id) REFERENCES product(product_id),
                           UNIQUE KEY uk_cart_product (cart_id, product_id) -- 동일 장바구니 내 동일 상품 중복 방지
);

-- 5. 포인트 이력 (Point_History)
CREATE TABLE point_history (
                               point_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               member_id BIGINT NOT NULL,
                               amount INT NOT NULL,
                               type VARCHAR(50) NOT NULL, -- EARN, USE, CANCEL 등
                               description VARCHAR(255),
                               created_at DATETIME NOT NULL,
                               FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- 6. 주문 (Orders)
-- 주문번호는 UUID 기반의 커스텀 문자열 사용 (ORD-XXXX...)
CREATE TABLE orders (
                        order_number VARCHAR(100) PRIMARY KEY,
                        member_id BIGINT NOT NULL,
                        status VARCHAR(50) NOT NULL, -- PENDING_PAYMENT, COMPLETED, CANCELLED 등
                        total_amount INT NOT NULL,
                        point_used INT DEFAULT 0,
                        pg_amount INT NOT NULL,
                        created_at DATETIME NOT NULL,
                        updated_at DATETIME NOT NULL,
                        FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- 7. 주문 상품 (Order_Item) - 스냅샷 포함
CREATE TABLE order_item (
                            order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_number VARCHAR(100) NOT NULL,
                            product_id BIGINT NOT NULL,
                            product_name VARCHAR(255) NOT NULL, -- 결제 시점 상품명 스냅샷
                            price_at_order INT NOT NULL,        -- 결제 시점 가격 스냅샷
                            quantity INT NOT NULL,
                            created_at DATETIME NOT NULL,
                            FOREIGN KEY (order_number) REFERENCES orders(order_number)
);

-- 8. 결제 내역 (Payment)
CREATE TABLE payment (
                         payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         order_number VARCHAR(100) NOT NULL UNIQUE,
                         portone_payment_id VARCHAR(100) NOT NULL UNIQUE, -- 포트원 결제 고유번호
                         status VARCHAR(50) NOT NULL, -- PAID, CANCELLED 등
                         amount INT NOT NULL,
                         method VARCHAR(50), -- CARD, POINT 등
                         created_at DATETIME NOT NULL,
                         FOREIGN KEY (order_number) REFERENCES orders(order_number)
);

-- 9. 빌링키 (Billing_Key) - 정기 구독용
CREATE TABLE billing_key (
                             billing_key_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             member_id BIGINT NOT NULL,
                             portone_billing_key VARCHAR(255) NOT NULL UNIQUE,
                             card_company VARCHAR(100),
                             is_active BOOLEAN DEFAULT TRUE,
                             created_at DATETIME NOT NULL,
                             updated_at DATETIME NOT NULL,
                             FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- 10. 정기 구독 (Subscription)
CREATE TABLE subscription (
                              subscription_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              member_id BIGINT NOT NULL,
                              billing_key_id BIGINT NOT NULL,
                              plan_name VARCHAR(100) NOT NULL,
                              status VARCHAR(50) NOT NULL, -- ACTIVE, PAUSED, CANCELED 등
                              next_payment_date DATE NOT NULL,
                              created_at DATETIME NOT NULL,
                              updated_at DATETIME NOT NULL,
                              FOREIGN KEY (member_id) REFERENCES member(member_id),
                              FOREIGN KEY (billing_key_id) REFERENCES billing_key(billing_key_id)
);