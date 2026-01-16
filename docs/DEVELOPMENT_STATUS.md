# 개발 진행 상태

**프로젝트**: MoneyFlow - 스마트 가계부 앱
**최종 업데이트**: 2026-01-16
**현재 단계**: MVP 개발 중 - 일괄 지출 API 추가, coupleId 제거 완료

---

## 📋 최근 업데이트 (2026-01-16)

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

1. OCR 기능 프론트엔드 통합
2. 일괄 지출 API를 활용한 OCR 결과 저장 구현
3. TestController 프로덕션 배포 전 제거
4. 통합 테스트 추가

---

## 📊 API 엔드포인트 현황

### 지출 관리
- `POST /api/expenses` - 지출 등록
- `POST /api/expenses/bulk` - **일괄 지출 생성 (신규, OCR용)**
- `GET /api/expenses` - 지출 목록 조회
- `GET /api/expenses/{id}` - 지출 상세 조회
- `PUT /api/expenses/{id}` - 지출 수정
- `DELETE /api/expenses/{id}` - 지출 삭제

### 테스트 (개발 전용)
- `GET /api/test/hash?password=xxx` - **BCrypt 해시 생성 (신규)**
- `GET /api/test/verify?password=xxx&hash=xxx` - **BCrypt 해시 검증 (신규)**

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

**마지막 수정**: 2026-01-16
**작성자**: MoneyFlow Development Team
