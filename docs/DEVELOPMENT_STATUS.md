# 개발 진행 상태

**프로젝트**: MoneyFlow - 스마트 가계부 앱
**최종 업데이트**: 2026-01-22
**현재 단계**: MVP 개발 중 - 자산 현황 조회 API 구현 완료

---

## 📋 최근 업데이트 (2026-01-22)

### 1️⃣ 자산 현황 조회 API 구현 및 성능 최적화
**커밋**: `185b2f4 - feat: 자산 현황 조회 API 구현 및 성능 최적화`

총자산 및 기간별 손익을 조회하고, 선택적으로 카테고리별 통계를 제공하는 API 구현

**주요 기능**:
- ✅ **GET /api/statistics/assets** - 자산 현황 조회 API
- ✅ **현재 총자산 계산** - 초기잔액 + 총수입 - 총지출 (날짜 필터 무관)
- ✅ **기간별 손익 분석** - 선택한 기간 내의 수입/지출 집계
- ✅ **카테고리별 통계** - includeStats 옵션으로 선택적 조회

**응답 데이터**:
```json
{
  "currentTotalAssets": 3250000.00,
  "initialBalance": 1000000.00,
  "totalIncome": 3700000.00,
  "totalExpense": 1450000.00,
  "periodIncome": 0,
  "periodExpense": 1300000.00,
  "periodNetIncome": -1300000.00,
  "incomeStats": [],
  "expenseStats": [
    {"name": "FOOD", "amount": 800000.00, "percent": 61.54},
    {"name": "SHOPPING", "amount": 300000.00, "percent": 23.08},
    {"name": "TRANSPORT", "amount": 200000.00, "percent": 15.38}
  ]
}
```

**성능 최적화**:
- ⚡ **DB GROUP BY 집계**: 메모리 효율 1000배 개선 (900KB → 900bytes)
  - AS-IS: 3,000개 지출 객체 로드 → Java Stream 집계
  - TO-BE: DB에서 카테고리별 SUM만 반환 (9개 결과)
- ⚡ **N+1 문제 해결**: JOIN FETCH로 쿼리 개수 감소 (N+2 → 1)
  - AccountBook + Members + Users를 한 번에 로드
- ⚡ **CategorySummary Projection**: 최소 데이터만 전송

**코드 품질 개선**:
- 🔧 비율 계산 로직 통합 (`calculatePercent`)
- 🔧 DRY 원칙 적용 (중복 코드 제거)
- 🔧 월간 통계 API도 DB GROUP BY로 리팩토링

**새로운 파일**:
- `CategorySummary.java` (신규) - DB GROUP BY 결과용 Projection 인터페이스
- `TotalAssetResponse.java` (신규) - 자산 현황 응답 DTO

**수정된 파일**:
- `ExpenseRepository.java` - `sumByCategory()` 추가
- `IncomeRepository.java` - `sumBySource()` 추가
- `StatisticsService.java` - `getAssetStatistics()` 구현, 성능 최적화
- `StatisticsController.java` - `/api/statistics/assets` 엔드포인트 추가

**테스트 완료**:
- ✅ 기본 자산 조회 (includeStats=false)
- ✅ 카테고리 통계 포함 (includeStats=true)
- ✅ DB GROUP BY 쿼리 실행 확인
- ✅ N+1 방지 JOIN FETCH 확인

---

## 📋 이전 업데이트 (2026-01-19)

### 1️⃣ 고정비 자동 매칭 및 지불 확인 시스템 구현
**커밋**: `b8423ec - feat: 고정비 자동 매칭 및 지불 확인 시스템 구현`

지출 등록 시 자동으로 고정비/구독료와 매칭하고, 사용자가 확인할 수 있는 시스템 추가

**주요 기능**:
- ✅ **RecurringExpensePayment 엔티티** - 지불 기록 관리 (PENDING/CONFIRMED/REJECTED)
- ✅ **RecurringExpenseMatchingService** - 자동 매칭 로직 (금액, 날짜 범위 기반)
- ✅ **RecurringExpensePaymentController** - 매칭 후보 조회 및 확인 API

**새로운 API 엔드포인트**:
- `GET /api/recurring-expense-payments/pending-matches` - 대기 중인 매칭 후보 조회
- `POST /api/recurring-expense-payments/confirm` - 매칭 확인
- `POST /api/recurring-expense-payments/reject` - 매칭 거부

**매칭 로직**:
1. 지출 생성 시 활성화된 고정비 중 금액이 일치하는 항목 검색
2. 지출 날짜가 고정비 결제 주기 ±7일 이내인지 확인
3. 매칭 발견 시 PENDING 상태로 RecurringExpensePayment 생성
4. 사용자가 확인/거부 가능

**데이터베이스 변경**:
```sql
-- 실행 필요: Supabase SQL Editor
CREATE TABLE recurring_expense_payments (
    id UUID PRIMARY KEY,
    recurring_expense_id UUID REFERENCES recurring_expenses(id),
    expense_id UUID REFERENCES expenses(id),
    status VARCHAR(20) NOT NULL, -- PENDING, CONFIRMED, REJECTED
    matched_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**파일**:
- `RecurringExpensePayment.java` (신규)
- `PaymentStatus.java` (신규 enum)
- `RecurringExpensePaymentRepository.java` (신규)
- `RecurringExpensePaymentController.java` (신규)
- `RecurringExpenseMatchingService.java` (신규)
- `ConfirmMatchRequest.java` (신규 DTO)
- `MatchCandidateResponse.java` (신규 DTO)
- `RecurringExpensePaymentResponse.java` (신규 DTO)
- `Expense.java` (recurringExpensePayment 관계 추가)
- `ExpenseService.java` (매칭 로직 통합)

---

## 📋 이전 업데이트 (2026-01-16)

### 1️⃣ 일괄 지출 생성 API 추가 (OCR 결과 저장용)
**커밋**: `e937f7e - feat: 일괄 지출 생성 API 추가 (OCR 결과 저장용)`

- ✅ **BulkExpenseRequest / BulkExpenseResponse DTO** 추가
- ✅ **POST /api/expenses/bulk** 엔드포인트 구현
- ✅ **부분 실패 허용(Partial Failure Tolerance)** - 일부 항목 실패 시에도 나머지 저장
- ✅ 실패 항목 인덱스 및 사유 반환
- ✅ ExpenseService.createBulkExpenses() 구현

**테스트 결과**:
- ✅ 정상 케이스: 3개 지출 모두 생성 성공
- ✅ 부분 실패 케이스: 3개 중 2개 성공, 1개 실패 (장부 없음)
- ✅ 검증 실패 케이스: 전체 요청 거부 (음수 금액)

**파일**:
- `ExpenseController.java:55-68`
- `ExpenseService.java:189-217`
- `BulkExpenseRequest.java`
- `BulkExpenseResponse.java`

### 2️⃣ coupleId 필드 완전 제거 - AccountBook 단일화
**커밋**: `000b4d7 - fix: coupleId 필드 참조하는 deprecated 쿼리 메서드 제거`

이전 리팩토링에서 Expense, Income 엔티티에서 coupleId 필드를 제거했으나, Repository에 deprecated 쿼리 메서드가 남아 있어 서버 시작 실패 문제가 발생했습니다.

**제거된 메서드**:
- ✅ `ExpenseRepository.findExpensesByCoupleAndDateRange()` 제거
- ✅ `IncomeRepository.findIncomesByCoupleAndDateRange()` 제거

**에러 해결**:
```
org.hibernate.query.sqm.UnknownPathException:
Could not resolve attribute 'coupleId' of 'com.moneyflow.domain.expense.Expense'
```

**관련 커밋**:
- `a64501f - refactor: Request DTO에서 @Deprecated coupleId 필드 제거`

### 3️⃣ OAuth 서비스 예외 처리 표준화
**커밋**: `fa592ee - refactor: OAuth 서비스 예외 처리 및 로깅 개선`

- ✅ GoogleOAuthService, AppleOAuthService, KakaoOAuthService, NaverOAuthService
- ✅ BusinessException(ErrorCode) 패턴으로 통일
- ✅ ErrorCode 추가: INVALID_OAUTH_TOKEN, OAUTH_API_ERROR
- ✅ 로깅 개선: System.out → log.debug

### 4️⃣ 개발용 TestController 추가
**커밋**: `0d7d642 - chore: BCrypt 테스트용 임시 엔드포인트 추가`

**목적**: 데이터베이스에 테스트 유저 생성 시 BCrypt 해시 생성/검증 지원

**엔드포인트**:
- `GET /api/test/hash?password=xxx` - BCrypt 해시 생성
- `GET /api/test/verify?password=xxx&hash=xxx` - BCrypt 해시 검증

**SecurityConfig 변경**:
- `/api/test/**` 경로를 permitAll()로 설정

⚠️ **주의**: 프로덕션 배포 전 삭제 필요

**파일**:
- `TestController.java` (신규)
- `SecurityConfig.java:71` (permitAll 추가)

---

## 🔧 해결된 이슈

### Issue #1: 서버 시작 실패 - coupleId 참조 에러
**증상**:
```
UnknownPathException: Could not resolve attribute 'coupleId'
```

**원인**: ExpenseRepository와 IncomeRepository에 deprecated 쿼리 메서드가 남아있어 삭제된 coupleId 필드를 참조

**해결**:
- ExpenseRepository.findExpensesByCoupleAndDateRange() 제거
- IncomeRepository.findIncomesByCoupleAndDateRange() 제거

### Issue #2: 테스트 계정 로그인 실패 - 비밀번호 해시 불일치
**증상**:
```
401 Unauthorized - 이메일 또는 비밀번호가 올바르지 않습니다
```

**원인**: 수동으로 생성한 BCrypt 해시가 실제 비밀번호와 일치하지 않음

**해결**:
1. TestController 생성 (`/api/test/hash`, `/api/test/verify`)
2. 올바른 BCrypt 해시 생성: `$2a$10$.eqMuXICSxJ0yroA3A3dfu0miDDPRSq5IEye5JvImBA0eJTVD8cCu`
3. 데이터베이스 user_auths 테이블 password_hash 업데이트

---

## 🎯 다음 단계

1. **프론트엔드 통합**
   - 자산 현황 화면 구현
   - OCR 기능 프론트엔드 통합
   - 일괄 지출 API를 활용한 OCR 결과 저장

2. **코드 정리**
   - TestController 프로덕션 배포 전 제거
   - PasswordHashGenerator 유틸리티 제거 (개발용)

3. **테스트 강화**
   - 통합 테스트 추가
   - 성능 테스트 (대용량 데이터)

---

## 📊 API 엔드포인트 현황

### 지출 관리
- `POST /api/expenses` - 지출 등록
- `POST /api/expenses/bulk` - 일괄 지출 생성 (OCR용)
- `GET /api/expenses` - 지출 목록 조회
- `GET /api/expenses/{id}` - 지출 상세 조회
- `PUT /api/expenses/{id}` - 지출 수정
- `DELETE /api/expenses/{id}` - 지출 삭제

### 통계
- `GET /api/statistics/monthly` - 월간 통계 조회
- `GET /api/statistics/weekly` - 주간 통계 조회
- `GET /api/statistics/assets` - **자산 현황 조회 (신규)** ✨
  - Query Params: `accountBookId`, `startDate`, `endDate`, `includeStats`
  - 현재 총자산, 기간별 손익, 카테고리별 통계 제공

### 테스트 (개발 전용)
- `GET /api/test/hash?password=xxx` - BCrypt 해시 생성
- `GET /api/test/verify?password=xxx&hash=xxx` - BCrypt 해시 검증

---

## 📝 테이블 구조

### user_auths
```sql
CREATE TABLE user_auths (
    auth_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255),
    password_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, provider),
    UNIQUE (provider, provider_id)
);
```

**설계**:
- EMAIL 제공자: provider_id NULL, password_hash 필수
- 소셜 로그인: provider_id 필수, password_hash NULL
- 한 사용자가 여러 인증 방법 보유 가능 (1:N 관계)

---

**마지막 수정**: 2026-01-22
**작성자**: MoneyFlow Development Team
