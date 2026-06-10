# Payment Commerce System

## 프로젝트 소개 (JoJo Pay)

JoJoPay는 일반 상품 구매와 정기 구독 서비스를 지원하는
결제 중심 커머스 플랫폼입니다.

단순 상품 주문 기능 구현에 그치지 않고

- 포인트 복합 결제
- 결제 검증
- 부분 환불
- 전체 환불
- 정기 구독 결제
- Billing Key 관리

등 실제 결제 서비스에서 요구되는 핵심 기능을 구현하는 것을 목표로 하였습니다.

또한 결제 데이터의 정합성과 추적 가능성을 확보하기 위해

- Cart / CartItem 분리
- Refund / RefundItem 분리
- Order / OrderItem 분리
- PointHistory 기반 이력 관리
- SubscriptionBilling 기반 구독 결제 이력 관리

구조를 설계하였습니다.

---

# 프로젝트 목표

저희 프로젝트는 다음과 같은 실무 역량 확보를 목표로 합니다.

* 실서비스 수준의 결제 도메인 설계 경험
* PG 연동 및 비동기 이벤트 처리 경험
* 트랜잭션 기반 데이터 정합성 보장
* 멱등성 기반 안정적인 결제 상태 관리
* 포인트 및 환불 도메인 설계 경험
* JWT 기반 인증/인가 구조 설계
* 협업 기반 API 설계 및 역할 분담 경험

---

# 기술 스택

## Backend

* Java 17
* Spring Boot
* Spring Security
* JPA (Hibernate)

## Database

* MySQL

## Authentication

* JWT

## Payment

* PortOne V2
* KG이니시스

## External API

* PortOne Payment API

## Infrastructure

* AWS EC2
* Docker
* GitHub Actions

## Collaboration

* GitHub

---

# 시스템 아키텍처

<img width="1232" height="804" alt="시스템 아키텍쳐" src="https://github.com/user-attachments/assets/574973c8-93f0-4579-8452-6a07f2ace33b" />

---

# 프로젝트 구조

src/main/java

com.team11.jojopay

├── auth

├── member

├── product

├── cart

├── cartitem

├── order

├── payment

├── point

├── refund

├── subscription

├── scheduler

├── common

│ ├── security

│ ├── exception

│ ├── response

│ └── config

│ └── entity

│ └── health

└── infrastructure

---

#API 명세

https://www.notion.so/teamsparta/API-36f2dc3ef51480e5801de83e56999aa0

---

ERD

<img width="1715" height="3192" alt="diagram" src="https://github.com/user-attachments/assets/2d085b21-4889-49ad-94ab-455023d15d84" />

---

# 주요 기능

## 회원

- 회원가입
- 로그인
- JWT 인증
- 회원 등급 관리

---

## 상품

### 상품 목록 조회

지원 기능

- 카테고리 필터링
- 가격 범위 필터링
- 판매 상태 필터링
- 최신순 정렬
- 가격 오름차순
- 가격 내림차순
- 페이지네이션

### 상품 상세 조회

조회 정보

- 상품명
- 가격
- 재고
- 설명
- 판매 상태
- 카테고리

---

## 장바구니

### 상품 담기

- 동일 상품 재담기 시 수량 합산
- 재고 초과 검증

### 장바구니 조회

- 상품 목록
- 수량 조회
- 총 금액 계산

### 수량 변경

- 장바구니 상품 수량 변경

### 상품 삭제

- 장바구니 상품 개별 삭제

### 장바구니 비우기

- 장바구니 상품 전체 삭제

Soft Delete 적용

---

## 주문

주문 생성 시

- Order
- OrderItem

을 분리하여 관리합니다.

### Order

주문 자체를 관리

### OrderItem

주문 당시 상품 스냅샷 저장

- 상품명
- 가격
- 수량

---

## 결제

PortOne 결제 연동

지원 기능

- 일반 결제
- 포인트 복합 결제
- 결제 검증
- 결제 취소

결제 상태

- READY
- COMPLETED
- FAILED
- CANCELED

---

## 포인트

포인트 이력 기반 관리

### 적립

EARN

### 사용

USE

### 사용 포인트 복구

USE_RECOVERY

### 적립 포인트 회수

EARN_FORFEIT

모든 포인트는 PointHistory에 기록됩니다.

---

## 환불

지원 기능

- 부분 환불
- 전체 환불

### Refund

환불 원장

### RefundItem

환불 상세 정보

환불 시

- 포인트 복구
- PG 취소

를 함께 처리합니다.

---

## 정기 구독

### BillingKey

정기 결제를 위한 결제수단 저장

### Subscription

구독 정보 관리

상태

- ACTIVE
- CANCELED
- PAST_DUE

### SubscriptionBilling

구독 결제 이력

- 성공 이력
- 실패 이력

---

# 인증 구조

JWT 기반 인증 방식을 사용합니다.

로그인 성공 시 Access Token을 발급하며,

모든 보호 API는 다음 형식으로 요청합니다.

Authorization: Bearer {AccessToken}

JwtAuthenticationFilter에서 토큰을 검증한 후

SecurityContext에 인증 정보를 저장합니다.

---

# 주문 및 결제 흐름

주문 생성 및 장바구니 저장

↓

재고 선차감

↓

PortOne 결제 요청

↓

결제 검증

↓

Payment 저장

↓

포인트 적립

↓

누적 결제 금액 반영

↓

결제 완료

---

# 환불 흐름

환불 요청

↓

환불 대상 상품 검증

↓

Refund 생성

↓

RefundItem 생성

↓

포인트 복구

↓

PortOne 결제 취소

↓

환불 완료

---

# 구독 결제 흐름

빌링키 등록

↓

구독 신청

↓

구독 정보 저장

↓

Scheduler 실행

↓

PortOne 정기 결제 요청

↓

결제 검증

↓

Payment 저장

↓

포인트 적립 및 누적 결제 금액 반영

---

# 설계 포인트

## 1. 도메인 책임 분리

Cart는 회원의 장바구니를 관리하고,

CartItem은 장바구니에 담긴 상품을 관리합니다.

마찬가지로 Order와 Refund도

도메인 책임 분리를 위해 별도 엔티티로 설계하였습니다.

---

## 2. 동일 상품 수량 합산

동일 상품을 여러 번 담을 경우

새로운 CartItem을 생성하지 않고 기존 수량에 합산합니다.

또한 DB 레벨에서

UNIQUE(cart_id, product_id)

제약조건을 적용하여 중복 데이터를 방지합니다.

---

## 3. Soft Delete

CartItem 삭제 시 실제 삭제하지 않고

deletedAt 컬럼을 기록합니다.

이를 통해

- 데이터 복구 가능
- 사용자 행위 추적 가능

구조를 구현하였습니다.

---

## 4. 주문 스냅샷 저장

상품 정보가 변경되더라도

주문 당시 정보를 보존할 수 있도록

OrderItem에

- 상품명
- 주문 가격
- 수량

을 저장합니다.

---

## 5. 포인트 이력 관리

포인트 잔액만 관리하지 않고

모든 변경 내역을 PointHistory에 저장합니다.

이를 통해

- 감사 로그 추적
- 포인트 변경 이력 조회
- 데이터 신뢰성 확보

가 가능합니다.

---

## 6. 정기 구독 설계

정기 구독 기능을

- BillingKey
- Subscription
- SubscriptionBilling

3개의 도메인으로 분리하였습니다.

이를 통해

- 구독 상태 관리
- 결제 이력 추적
- 결제 실패 대응

이 가능합니다.

---

# Technical Challenges

## 결제 데이터 정합성

## 문제

결제 승인 이후 DB 저장 과정에서 오류가 발생할 경우

결제는 성공했지만 주문 상태가 저장되지 않는 문제가 발생할 수 있습니다.

## 해결

- 결제 상태를 별도로 관리
- 검증 로직을 추가
- 트랜잭션 경계를 명확히 분리

## 결과

결제 상태와 주문 상태 간 데이터 정합성을 확보하였습니다.

---

## 주문 데이터 보존 문제

## 문제

상품 가격이나 상품명이 변경되면 과거 주문 정보가 변경될 수 있습니다.

## 해결

OrderItem에 주문 당시 정보를 스냅샷으로 저장하였습니다.

## 결과

과거 주문 내역의 무결성을 보장할 수 있습니다.

---

## 포인트 복합 결제

## 문제

포인트와 PG 결제를 동시에 사용하는 경우

포인트 차감과 결제 승인 간 정합성 문제가 발생할 수 있습니다.

## 해결

하나의 결제 흐름 안에서

- 포인트 차감
- 결제 승인
- 포인트 적립

을 관리하도록 설계하였습니다.

## 결과

결제 실패 시 데이터 불일치를 최소화하였습니다.

---

# Trouble Shooting

## Trouble Shooting 1
## Lost Update 문제 해결

## Before

<img width="1508" height="707" alt="1" src="https://github.com/user-attachments/assets/0138a1e4-5ed0-4953-aa1c-91699d5f318d" />

## After

<img width="1471" height="567" alt="1-2" src="https://github.com/user-attachments/assets/ed7efb08-aa1b-4c96-96a3-f06a3b1b743b" />

---

## Trouble Shooting 2
## 외부 API 호출과 DB 트랜잭션 분리

## Before

<img width="1372" height="601" alt="2" src="https://github.com/user-attachments/assets/0ac7949a-ba6d-4127-bdcd-d5ceebdd227c" />

## After

<img width="1526" height="494" alt="2-1" src="https://github.com/user-attachments/assets/f5161338-ffa2-4eb2-8579-fae63a7e9563" />


---

# 향후 개선 예정

* Redis 기반 분산 락 적용
* Kafka 기반 이벤트 드리븐 아키텍처
* 모니터링 및 알림 시스템 구축
* 테스트 커버리지 확대
* 구독 결제 실패 재시도 정책 추가
* 결제 이벤트 비동기 처리 고도화

---

