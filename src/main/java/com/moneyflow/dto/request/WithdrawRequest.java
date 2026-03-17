package com.moneyflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회원 탈퇴 요청 DTO
 *
 * 이메일 회원은 비밀번호 확인 필요
 * SNS 회원은 비밀번호 없이 탈퇴 가능
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원 탈퇴 요청")
public class WithdrawRequest {

    @Schema(description = "비밀번호 (이메일 회원만 필수)", example = "Password123!")
    private String password;

    @Schema(description = "탈퇴 사유 (선택)", example = "서비스를 더 이상 사용하지 않음")
    private String reason;
}
