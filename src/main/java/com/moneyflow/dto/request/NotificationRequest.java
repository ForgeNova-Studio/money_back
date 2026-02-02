package com.moneyflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 알림 생성 요청 DTO (관리자용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    // 특정 사용자 알림 시 필요, 전체 알림(send-all) 시 null 허용
    private UUID targetUserId;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 255, message = "제목은 255자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String message;

    @Builder.Default
    private String type = "PERSONAL";
}
