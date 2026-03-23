package com.moneyflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 상태에서 비밀번호 변경 요청 DTO
 *
 * 현재 비밀번호 확인 후 새 비밀번호로 변경
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "비밀번호 변경 요청 (로그인 상태)")
public class ChangePasswordWhileLoggedInRequest {

    @NotBlank(message = "현재 비밀번호는 필수입니다")
    @Schema(description = "현재 비밀번호", example = "CurrentPassword123!")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "비밀번호는 대문자, 소문자, 숫자, 특수문자(@$!%*?&)를 각각 최소 1개 이상 포함해야 합니다"
    )
    @Schema(description = "새 비밀번호 (8자 이상, 대소문자/숫자/특수문자 포함)", example = "NewPassword123!")
    private String newPassword;
}
