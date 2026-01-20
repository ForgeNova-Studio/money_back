package com.moneyflow.dto.request;

import com.moneyflow.domain.accountbook.FundingSource;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequest {

    @NotNull(message = "금액은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "금액은 0보다 커야 합니다")
    private BigDecimal amount;

    @NotNull(message = "날짜는 필수입니다")
    private LocalDate date;

    /**
     * 카테고리 (선택) - 미입력 시 자동 분류
     */
    @Size(max = 50)
    private String category;

    @Size(max = 255)
    private String merchant;

    private String memo;

    @Size(max = 20)
    private String paymentMethod;

    private String imageUrl;

    /**
     * 소속 장부 ID
     */
    private UUID accountBookId;

    /**
     * 지출 출처 (PERSONAL: 개인, SHARED_POOL: 공금)
     */
    private FundingSource fundingSource;

    /**
     * 실제 결제한 사람 ID (SHARED_POOL일 때 사용)
     * - null이면 등록자(user)가 결제한 것으로 처리
     */
    private UUID paidByUserId;

    /**
     * 참여자 목록 (SHARED_POOL일 때 N빵 대상자)
     * - null 또는 빈 리스트면 장부의 모든 멤버가 균등 분담
     */
    private List<ParticipantInfo> participants;

    private Boolean isAutoCategorized;

    /**
     * 참여자 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        /**
         * 참여자 사용자 ID
         */
        @NotNull
        private UUID userId;

        /**
         * 분담 비율 (기본값 1.0 = 균등 분배)
         */
        @Builder.Default
        private BigDecimal shareRatio = BigDecimal.ONE;
    }
}
