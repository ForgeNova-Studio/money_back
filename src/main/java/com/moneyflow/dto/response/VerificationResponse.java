package com.moneyflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "인증 응답")
public class VerificationResponse {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "메시지", example = "인증 코드가 발송되었습니다")
    private String message;

    public static VerificationResponse success(String message) {
        return VerificationResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static VerificationResponse failure(String message) {
        return VerificationResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
