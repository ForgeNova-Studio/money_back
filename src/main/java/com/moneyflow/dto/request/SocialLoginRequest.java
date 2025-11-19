package com.moneyflow.dto.request;

import com.moneyflow.domain.user.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 소셜 로그인 요청 DTO
 * 클라이언트에서 소셜 로그인 후 받은 ID Token을 서버에 전송
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "소셜 로그인 요청")
public class SocialLoginRequest {

    @NotNull(message = "로그인 제공자는 필수입니다")
    @Schema(description = "로그인 제공자", example = "GOOGLE", allowableValues = {"GOOGLE", "APPLE"})
    private AuthProvider provider;

    @NotBlank(message = "ID Token은 필수입니다")
    @Schema(description = "소셜 로그인 제공자로부터 받은 ID Token", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
    private String idToken;

    @Schema(description = "닉네임 (신규 가입 시 사용, 선택)", example = "홍길동")
    private String nickname;
}
