package com.moneyflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 닉네임 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "닉네임 수정 요청")
public class NicknameUpdateRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    @Schema(description = "변경할 닉네임", example = "머니플로우")
    private String nickname;
}
