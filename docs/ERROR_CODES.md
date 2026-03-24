# Error Codes

`moamoa_back`의 표준 에러 코드 문서입니다.

- 소스 오브 트루스: [src/main/java/com/moneyflow/exception/ErrorCode.java](/Users/hanwool/ground/ForgeNova_Lab/moamoa/moamoa_back/src/main/java/com/moneyflow/exception/ErrorCode.java)
- 공통 응답 생성: [src/main/java/com/moneyflow/exception/GlobalExceptionHandler.java](/Users/hanwool/ground/ForgeNova_Lab/moamoa/moamoa_back/src/main/java/com/moneyflow/exception/GlobalExceptionHandler.java)
- 프론트 특별 처리: [moamoa_front/lib/features/common/providers/dio_provider.dart](/Users/hanwool/ground/ForgeNova_Lab/moamoa/moamoa_front/lib/features/common/providers/dio_provider.dart)

## 응답 형식

```json
{
  "status": 400,
  "code": "A012",
  "message": "인증 시간이 만료되었습니다. 다시 인증해주세요",
  "timestamp": "2026-03-24T16:00:00"
}
```

검증 오류의 경우 `errors` 필드가 추가될 수 있습니다.

```json
{
  "status": 400,
  "code": "C002",
  "message": "입력값 검증에 실패했습니다",
  "timestamp": "2026-03-24T16:00:00",
  "errors": {
    "email": "이메일 형식이 올바르지 않습니다"
  }
}
```

## 코드 규칙

| Prefix | Domain | 설명 |
| --- | --- | --- |
| `C` | Common | 공통 입력값/서버 오류 |
| `A` | Auth | 인증, 로그인, 이메일 인증 |
| `U` | User | 사용자 정보 |
| `AB` | Account Book | 장부 |
| `E` | Expense | 지출 |
| `I` | Income | 수입 |
| `R` | Recurring | 고정비 |
| `AS` | Asset | 자산 |
| `CP` | Couple | 커플 |

## 전체 코드 목록

### Common

| Code | Name | HTTP | Default Message | 비고 |
| --- | --- | --- | --- | --- |
| `C001` | `INVALID_INPUT` | `400` | 입력값이 올바르지 않습니다 | 범용 입력 오류. 명시적 코드 없이 `new BusinessException(String message)`를 쓰면 기본적으로 이 코드로 내려갑니다. |
| `C002` | `VALIDATION_ERROR` | `400` | 입력값 검증에 실패했습니다 | `@Valid`, `@Validated` 계열 검증 실패 |
| `C003` | `INTERNAL_ERROR` | `500` | 서버 오류가 발생했습니다 | 예상하지 못한 런타임/일반 예외 |

### Auth

| Code | Name | HTTP | Default Message | 비고 |
| --- | --- | --- | --- | --- |
| `A001` | `INVALID_CREDENTIALS` | `401` | 이메일 또는 비밀번호가 올바르지 않습니다 | 이메일 로그인 실패 |
| `A002` | `TOKEN_EXPIRED` | `401` | 인증이 만료되었습니다 | 액세스/리프레시 토큰 만료 |
| `A003` | `ACCESS_DENIED` | `403` | 접근 권한이 없습니다 | 인증은 되었지만 권한 없음 |
| `A004` | `EMAIL_ALREADY_EXISTS` | `409` | 이미 사용 중인 이메일입니다 | 일반 회원가입 중복 이메일 |
| `A005` | `INVALID_OAUTH_TOKEN` | `401` | 유효하지 않은 소셜 로그인 토큰입니다 | Google/Kakao/Naver 토큰 검증 실패 |
| `A006` | `OAUTH_API_ERROR` | `502` | 소셜 로그인 서비스 오류가 발생했습니다 | 외부 OAuth API 호출 실패 |
| `A007` | `AUTHENTICATION_ERROR` | `401` | 인증에 실패했습니다 | 인증 엔트리포인트 기본 응답. 일부 토큰 만료 상황의 fallback으로도 사용 중 |
| `A008` | `EMAIL_REGISTERED_WITH_OTHER_PROVIDER` | `409` | 이 이메일은 다른 로그인 방법으로 가입되어 있습니다 | 소셜/이메일 가입 방식 충돌 |
| `A009` | `VERIFICATION_REQUIRED` | `400` | 인증을 먼저 완료해주세요 | 회원가입/비밀번호 재설정 전 인증 미완료 |
| `A010` | `VERIFICATION_CODE_NOT_FOUND` | `400` | 인증 코드를 찾을 수 없습니다 | 인증코드 레코드 없음 |
| `A011` | `VERIFICATION_CODE_EXPIRED` | `400` | 인증 코드가 만료되었습니다. 다시 요청해주세요. | 코드 자체 만료 |
| `A012` | `VERIFICATION_SESSION_EXPIRED` | `400` | 인증 시간이 만료되었습니다. 다시 인증해주세요 | 인증 완료 후 후속 액션 지연으로 세션 만료 |
| `A013` | `VERIFICATION_CODE_MISMATCH` | `400` | 인증 코드가 일치하지 않습니다 | 코드 불일치 |
| `A014` | `VERIFICATION_ATTEMPTS_EXCEEDED` | `400` | 인증 시도 횟수를 초과했습니다. 코드를 다시 요청해주세요. | 최대 시도 횟수 초과 |

### User

| Code | Name | HTTP | Default Message | 비고 |
| --- | --- | --- | --- | --- |
| `U001` | `USER_NOT_FOUND` | `404` | 사용자를 찾을 수 없습니다 | 사용자 조회 실패 |
| `U002` | `INVALID_PASSWORD` | `400` | 비밀번호가 올바르지 않습니다 | 비밀번호 변경/탈퇴 등 |
| `U003` | `INVALID_NICKNAME` | `400` | 유효하지 않은 닉네임입니다 | 닉네임 형식/길이 오류 |

### Account Book

| Code | Name | HTTP | Default Message | 비고 |
| --- | --- | --- | --- | --- |
| `AB001` | `ACCOUNT_BOOK_NOT_FOUND` | `404` | 장부를 찾을 수 없습니다 | 장부 없음 |
| `AB002` | `ACCOUNT_BOOK_REQUIRED` | `400` | 장부 ID는 필수입니다 | 요청 파라미터 누락 |
| `AB003` | `ACCOUNT_BOOK_ACCESS_DENIED` | `403` | 해당 장부에 접근할 권한이 없습니다 | 장부 접근 권한 없음 |

### Expense

| Code | Name | HTTP | Default Message | 비고 |
| --- | --- | --- | --- | --- |
| `E001` | `EXPENSE_NOT_FOUND` | `404` | 지출 내역을 찾을 수 없습니다 | 지출 상세 조회/수정/삭제 실패 |

### Income

| Code | Name | HTTP | Default Message | 비고 |
| --- | --- | --- | --- | --- |
| `I001` | `INCOME_NOT_FOUND` | `404` | 수입 내역을 찾을 수 없습니다 | 수입 상세 조회/수정/삭제 실패 |

### Recurring

| Code | Name | HTTP | Default Message | 비고 |
| --- | --- | --- | --- | --- |
| `R001` | `RECURRING_EXPENSE_NOT_FOUND` | `404` | 고정비를 찾을 수 없습니다 | 고정비 조회 실패 |

### Asset

| Code | Name | HTTP | Default Message | 비고 |
| --- | --- | --- | --- | --- |
| `AS001` | `ASSET_NOT_FOUND` | `404` | 자산을 찾을 수 없습니다 | 자산 조회 실패 |

### Couple

| Code | Name | HTTP | Default Message | 비고 |
| --- | --- | --- | --- | --- |
| `CP001` | `COUPLE_NOT_FOUND` | `404` | 커플 정보를 찾을 수 없습니다 | 커플 조회 실패 |
| `CP002` | `ALREADY_COUPLED` | `409` | 이미 커플이 연결되어 있습니다 | 이미 커플 연결 상태 |

## 프론트에서 특별 처리하는 코드

현재 프론트는 아래 코드를 일반 토스트 외의 흐름 제어에 사용합니다.

| Code | 프론트 동작 |
| --- | --- |
| `A002` | 토큰 만료로 간주하고 갱신 시도 |
| `A007` | 백엔드 이슈 대응용 토큰 만료 fallback으로 간주하고 갱신 시도 |
| `U001` | 자동 로그아웃 처리 |
| `AB003` | 로그아웃하지 않고 에러만 전달 |
| `A012` | 회원가입 화면에서 이메일 인증 단계만 초기화 |

## 예외 매핑 규칙

### `BusinessException`

- `new BusinessException(ErrorCode.X)` 또는 `new BusinessException(message, ErrorCode.X)`를 사용하면 해당 코드로 응답합니다.
- `new BusinessException(String message)`만 사용하면 하위 호환 때문에 `C001`로 응답합니다.

즉, 도메인별 복구 플로우가 필요한 예외는 반드시 `ErrorCode`를 명시해야 합니다.

### `UnauthorizedException`

- `UnauthorizedException.authentication(...)`는 기본적으로 `A007`
- `UnauthorizedException.accessDenied(...)`는 기본적으로 `A003`

### Spring Validation

- `MethodArgumentNotValidException`
- `ConstraintViolationException`

위 두 경우는 모두 `C002`로 응답합니다.

## 운영 주의사항

1. 사용자 복구 흐름이 필요한 예외는 문자열 메시지 분기 대신 `code` 분기를 사용합니다.
2. 새 예외를 추가할 때는 `ErrorCode.java`와 이 문서를 함께 갱신합니다.
3. `C001`은 너무 범용적이므로, 인증/회원가입/권한/리소스 관련 예외에 남용하지 않습니다.
4. 인증 관련 예외는 가능하면 `A` 대역으로 구체화합니다.
