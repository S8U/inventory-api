# 재고 관리 API

상품 입고, 출고, 재고 조회 API

Java 21, Spring Boot 4.1.2, Spring Data JPA, PostgreSQL 18, Flyway, Testcontainers

## 실행 방법

JDK 21과 Docker가 필요합니다.

```bash
./gradlew bootRun   # compose.yaml의 PostgreSQL이 함께 실행됨
./gradlew test      # Testcontainers로 PostgreSQL 실행
```

Swagger UI: http://localhost:8080/swagger-ui/index.html

## 프로젝트 구조

```
src/main/java/com/github/s8u/inventory
├── controller/   API, 요청 검증
├── usecase/      입고, 출고, 조회 (기능마다 클래스 하나, 트랜잭션 경계)
├── entity/       상품, 재고 변동 이력 (재고 증감 규칙)
├── repository/
├── exception/    에러 코드, 예외 처리
├── enums/
└── config/
```

레이어드 구조를 바탕으로 클린 아키텍처와 DDD의 일부를 가져왔습니다.

책임을 끝까지 나누는 클린 아키텍처, 헥사고날이 가장 좋다고 보지만 계층이 많아 만드는 데 오래 걸립니다.

AI로 코드를 작성하면서 이 부담은 줄었지만, 코드는 사람이 검토해야 하고 계층이 늘수록 검토할 코드도 늘어납니다.

그래서 효과가 큰 부분만 가져왔습니다.

- 기능마다 UseCase 클래스를 둡니다. 도메인마다 Service 하나에 모으면 기능이 늘수록 의존성과 트랜잭션 범위가 섞이기 때문입니다.
- 재고 증감 규칙은 엔티티 메서드에 둡니다. 어느 기능에서 재고를 바꾸든 같은 규칙을 거치고, DB 없이 테스트할 수 있습니다.
- 포트와 어댑터는 두지 않았습니다. 지금은 외부 경계가 DB 하나뿐이라, 바뀔 경계가 생기면 그곳에만 둡니다.

## Schema

DDL: [docs/schema.sql](docs/schema.sql)

| 테이블 | 설명 | 제약 |
|---|---|---|
| `product` | 상품과 현재 재고 | `sku` UNIQUE, `quantity >= 0` |
| `product_stock_history` | 입고, 출고 이력 (변동 전후 재고) | `quantity > 0` |

상품 식별자로 `sku`를 따로 두었습니다.

- 입고는 등록 전 상품도 받아야 하는데, DB가 만드는 `id`는 등록 전에는 알 수 없습니다.
- 상품명은 바뀌거나 겹칠 수 있어 식별자로 쓰지 않았습니다.

## API Spec

상품은 SKU로 식별합니다. `id`는 DB 내부 키로만 씁니다.

| 기능 | 요청 | 본문 |
|---|---|---|
| 입고 | `POST /api/products/{sku}/inbound` | `{ "name": "생수 500ml", "quantity": 10 }` |
| 출고 | `POST /api/products/{sku}/outbound` | `{ "quantity": 3 }` |
| 재고 조회 | `GET /api/products/{sku}` | |

- 입고: 없는 SKU면 상품을 등록한 뒤 입고합니다. 이때만 `name`이 필요합니다.
- 출고: 재고보다 많이 출고할 수 없습니다.

응답

```json
{ "sku": "WTR-500", "name": "생수 500ml", "quantity": 7 }
```

에러 응답 ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) + `code`)

```json
{
  "title": "Conflict",
  "status": 409,
  "detail": "재고가 부족합니다.",
  "instance": "/api/products/WTR-500/outbound",
  "code": "PRODUCT_STOCK_INSUFFICIENT"
}
```

| 상태 | code | 설명 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 요청 값 오류 |
| 400 | `PRODUCT_NAME_REQUIRED` | 신규 상품 입고에 상품명 없음 |
| 404 | `PRODUCT_NOT_FOUND` | 상품 없음 |
| 409 | `PRODUCT_STOCK_INSUFFICIENT` | 재고 부족 |
| 409 | `PRODUCT_STOCK_OVERFLOW` | 재고 최대값 초과 |
| 500 | `INTERNAL_ERROR` | 서버 오류 |

## 동시성 처리

- 같은 상품의 입고, 출고는 상품 행 비관적 락(`SELECT ... FOR UPDATE`)으로 순서대로 처리합니다.
- 같은 신규 SKU가 동시에 입고되면 `INSERT ... ON CONFLICT DO NOTHING`으로 상품을 하나만 등록합니다.
- DB의 `CHECK (quantity >= 0)`로 음수 재고를 한 번 더 막습니다.

## 테스트케이스

동시성은 실제 PostgreSQL(Testcontainers)에서 여러 스레드로 동시에 요청해 확인합니다.

확인하려는 것이 행 락 대기, UNIQUE 충돌 처리 같은 DB 엔진 동작이라 H2 같은 다른 DB로는 검증할 수 없습니다.

| 시나리오 | 요청 | 결과 |
|---|---|---|
| 동시 입고 | 재고 0, 입고 1개 × 100 | 재고 100 |
| 신규 SKU 동시 입고 | 새 SKU 50개, 입고 5개 × 2 | 상품 50개, 각 재고 10 |
| 동시 출고 | 재고 100, 출고 1개 × 100 | 재고 0 |
| 재고 초과 동시 출고 | 재고 50, 출고 1개 × 100 | 성공 50, 실패 50, 재고 0 |
| 동시 입고와 출고 | 재고 100, 입고 1개 × 50, 출고 1개 × 50 | 재고 100 |

락이나 `ON CONFLICT`를 빼면 해당 테스트가 실패하는 것을 확인했습니다.

그 밖에 재고 증감 규칙(엔티티), 유스케이스, 요청 검증과 에러 응답(컨트롤러) 단위 테스트가 있습니다.
