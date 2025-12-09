package com.moneyflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 변경 요청 DTO
 *
 * 인증 코드 검증 완료 후 비밀번호만 변경할 때 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "비밀번호 변경 요청 (인증 완료 후)")
public class ChangePasswordRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "이메일 주소", example = "user@example.com")
    private String email;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    @Schema(description = "새 비밀번호 (8자 이상)", example = "newPassword123!")
    private String newPassword;
}
