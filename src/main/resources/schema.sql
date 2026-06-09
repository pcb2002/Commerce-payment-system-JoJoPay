-- MEMBER
CREATE TABLE IF NOT EXISTS member (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    point_balance BIGINT DEFAULT 0,
    total_payment_amount BIGINT DEFAULT 0,
    membership_grade VARCHAR(20),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
    );

-- PRODUCT
CREATE TABLE IF NOT EXISTS product (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       name VARCHAR(255) NOT NULL,
    price BIGINT NOT NULL,
    stock INTEGER NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
    );

-- CART
CREATE TABLE IF NOT EXISTS cart (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    member_id BIGINT NOT NULL UNIQUE,
                                    created_at DATETIME NOT NULL,
                                    updated_at DATETIME NOT NULL,
                                    FOREIGN KEY (member_id) REFERENCES member(id)
    );

-- CART_ITEM
CREATE TABLE IF NOT EXISTS cart_item (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         cart_id BIGINT NOT NULL,
                                         product_id BIGINT NOT NULL,
                                         quantity INTEGER NOT NULL,
                                         deleted_at DATETIME,
                                         created_at DATETIME NOT NULL,
                                         updated_at DATETIME NOT NULL,
                                         FOREIGN KEY (cart_id) REFERENCES cart(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
    );

-- BILLING_KEY
CREATE TABLE IF NOT EXISTS billing_key (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           member_id BIGINT NOT NULL,
                                           customer_uid VARCHAR(255) NOT NULL,
    card_name VARCHAR(50),
    card_number VARCHAR(20),
    status VARCHAR(20),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (member_id) REFERENCES member(id)
    );

-- SUBSCRIPTION
CREATE TABLE IF NOT EXISTS subscription (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            member_id BIGINT NOT NULL,
                                            billing_key_id BIGINT NOT NULL,
                                            plan_name VARCHAR(50) NOT NULL,
    price BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    next_billing_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (billing_key_id) REFERENCES billing_key(id)
    );

-- SUBSCRIPTION_BILLING
CREATE TABLE IF NOT EXISTS subscription_billing (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    subscription_id BIGINT NOT NULL,
                                                    billing_cycle INTEGER,
                                                    billing_period VARCHAR(50),
    amount BIGINT NOT NULL,
    billing_status VARCHAR(20),
    portone_tier_payment_id VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (subscription_id) REFERENCES subscription(id)
    );

-- ORDERS
-- ORDERS
CREATE TABLE IF NOT EXISTS orders (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 이 부분이 있어야 합니다!
                                      order_number VARCHAR(100) NOT NULL,
    member_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount BIGINT NOT NULL,
    used_point BIGINT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (member_id) REFERENCES member(id)
    );

-- ORDER_ITEM
CREATE TABLE IF NOT EXISTS order_item (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          order_id BIGINT NOT NULL,
                                          product_id BIGINT NOT NULL,
                                          product_name VARCHAR(255) NOT NULL,
    price_at_order BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(20),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
    );

-- PAYMENT
CREATE TABLE IF NOT EXISTS payment (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       order_id BIGINT NOT NULL,
                                       member_id BIGINT NOT NULL,
                                       subscription_id BIGINT,
                                       portone_payment_id VARCHAR(100) NOT NULL,
    amount BIGINT NOT NULL,
    pg_real_amount BIGINT NOT NULL,
    used_point BIGINT DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    pg_provider VARCHAR(50),
    payment_method VARCHAR(30),
    approved_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (subscription_id) REFERENCES subscription(id)
    );

-- POINT_HISTORY
CREATE TABLE IF NOT EXISTS point_history (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             member_id BIGINT NOT NULL,
                                             payment_id BIGINT,
                                             transaction_type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (member_id) REFERENCES member(id),
    FOREIGN KEY (payment_id) REFERENCES payment(id)
    );

-- REFUND
CREATE TABLE IF NOT EXISTS refund (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      payment_id BIGINT NOT NULL,
                                      reason VARCHAR(255),
    total_refund_amount BIGINT NOT NULL,
    point_refund_amount BIGINT DEFAULT 0,
    pg_refund_amount BIGINT DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (payment_id) REFERENCES payment(id)
    );

-- REFUND_ITEM
CREATE TABLE IF NOT EXISTS refund_item (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           refund_id BIGINT NOT NULL,
                                           order_item_id BIGINT NOT NULL,
                                           quantity INTEGER NOT NULL,
                                           created_at DATETIME NOT NULL,
                                           updated_at DATETIME NOT NULL,
                                           FOREIGN KEY (refund_id) REFERENCES refund(id),
    FOREIGN KEY (order_item_id) REFERENCES order_item(id)
    );