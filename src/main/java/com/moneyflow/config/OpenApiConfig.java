package com.moneyflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 설정
 * 기능:
 * - Swagger UI 문서 메타데이터 설정
 * - JWT Bearer 토큰 인증 스키마 설정
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT 인증 스키마 이름
        String jwtSchemeName = "bearerAuth";

        // Security Requirement 설정 (모든 API에 JWT 인증 필요)
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);

        // Security Scheme 설정 (JWT Bearer 토큰)
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요 (Bearer 접두사 제외)")
                );

        // API 문서 상세 설명
        String description = """
                # MoneyFlow API 문서

                스마트 가계부 앱의 백엔드 REST API입니다.

                ## 📋 테스트 방법d

                ### 1️⃣ 로그인하여 JWT 토큰 받기

                1. **Auth** 섹션에서 `POST /api/auth/login` API를 찾습니다
                2. **Try it out** 버튼을 클릭합니다
                3. Request body에 아래 테스트 계정 정보를 입력합니다:

                ```json
                {
                  "email": "testuser1@test.com",
                  "password": "password"
                }
                ```

                4. **Execute** 버튼을 클릭합니다
                5. Response에서 `accessToken` 값을 **복사**합니다
                   - 예시: `eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIzNjY4ODNh...`

                ### 2️⃣ Swagger UI에 JWT 토큰 설정하기

                1. **페이지 우측 상단의 Authorize 🔓 버튼**을 클릭합니다
                2. **bearerAuth** 입력란에 **토큰만** 붙여넣습니다
                   - ⚠️ **주의**: "Bearer " 접두사는 입력하지 마세요!
                   - ✅ 올바른 예: `eyJhbGciOiJIUzUxMiJ9.eyJzdWIi...`
                   - ❌ 잘못된 예: `Bearer eyJhbGciOiJIUzUxMiJ9...`
                3. **Authorize** 버튼을 클릭합니다
                4. **Close** 버튼을 클릭합니다

                ### 3️⃣ API 테스트하기

                이제 모든 🔒 표시가 있는 보호된 API를 자유롭게 호출할 수 있습니다!

                각 API의 **Try it out** 버튼을 클릭하고 파라미터를 입력한 후 **Execute**하세요.

                ## 🧪 테스트 계정 정보

                ### 개인 사용자 계정 1
                - **이메일**: `testuser1@test.com`
                - **비밀번호**: `password`
                - **닉네임**: 테스트유저1
                - **설명**: 기본 테스트 계정입니다. 장부 생성 및 지출 관리를 테스트할 수 있습니다.

                ### 개인 사용자 계정 2
                - **이메일**: `testuser2@test.com`
                - **비밀번호**: `password`
                - **닉네임**: 테스트유저2
                - **설명**: 커플 모드 테스트용 계정입니다. testuser1과 커플 연동할 수 있습니다.

                ## 📌 주요 기능별 테스트 시나리오

                ### ✅ 지출 관리 테스트
                1. `POST /api/expenses` - 새 지출 등록
                2. `GET /api/expenses` - 지출 목록 조회 (날짜/카테고리 필터 사용 가능)
                3. `GET /api/expenses/{expenseId}` - 특정 지출 상세 조회
                4. `PUT /api/expenses/{expenseId}` - 지출 수정
                5. `DELETE /api/expenses/{expenseId}` - 지출 삭제

                ### 📊 통계 테스트
                1. `GET /api/statistics/monthly` - 월간 통계 조회
                   - 예시: `year=2025`, `month=11`
                2. `GET /api/statistics/weekly` - 주간 통계 조회
                   - 예시: `startDate=2025-11-11`

                ### 💑 커플 모드 테스트
                1. `POST /api/couples/invite` - 초대 코드 생성 (testuser1 계정)
                2. `POST /api/couples/join` - 초대 코드로 가입 (testuser2 계정으로 로그인 후)
                3. `GET /api/couples/me` - 현재 커플 정보 조회
                4. `GET /api/account-books` - "우리의 생활비" 기본 장부 자동 생성 확인
                5. `DELETE /api/couples/unlink` - 커플 연동 해제

                ### 📒 장부 관리 테스트
                1. `POST /api/account-books` - 새 장부 생성
                   - 예시: `{"name":"제주도 여행","bookType":"TRIP","memberCount":2,"description":"2026년 1월 제주도 여행"}`
                2. `GET /api/account-books` - 내 장부 목록 조회
                3. `GET /api/account-books/{accountBookId}` - 장부 상세 조회
                4. `POST /api/expenses` - 지출 등록 시 accountBookId 연결
                   - 예시: `{"amount":35000,"category":"FOOD","merchant":"성산 해녀의집","date":"2026-01-20","accountBookId":"장부ID","fundingSource":"SHARED_POOL"}`
                5. `POST /api/account-books/{accountBookId}/members` - 멤버 추가

                ## ⚠️ 참고사항

                - **JWT 토큰 유효기간**: 1시간 (만료 시 다시 로그인 필요)
                - **날짜 형식**: `yyyy-MM-dd` (예: 2025-11-17)
                - **금액 형식**: 숫자만 입력 (예: 15000)
                - **카테고리 목록**: 식비, 교통, 쇼핑, 문화생활, 주거/통신, 의료/건강, 교육, 경조사, 기타

                ## 🐛 문제 해결

                ### 403 Forbidden 에러가 발생하는 경우
                - JWT 토큰이 설정되지 않았거나 만료되었습니다
                - 위의 1️⃣, 2️⃣ 단계를 다시 진행하세요

                ### 401 Unauthorized 에러가 발생하는 경우
                - 로그인 정보가 잘못되었습니다
                - 테스트 계정 정보를 확인하세요

                ### 404 Not Found 에러가 발생하는 경우
                - 존재하지 않는 리소스 ID입니다
                - GET API로 먼저 목록을 조회한 후 실제 ID를 사용하세요

                ## 📞 문의

                API 사용 중 문제가 있으면 백엔드 팀에게 문의하세요.
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("MoneyFlow API")
                        .description(description)
                        .version("1.0.0")
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버")
                ))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
