package com.moneyflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 커플 가입 요청 DTO
 *
 * 초대 코드를 입력하여 커플에 가입하는 요청
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinCoupleRequest {

    /**
     * 초대 코드 (6자리 영숫자)
     * 예: "A3B9C2"
     */
    @NotBlank(message = "초대 코드를 입력해주세요")
    @Size(min = 6, max = 10, message = "초대 코드는 6~10자리여야 합니다")
    private String inviteCode;
}
