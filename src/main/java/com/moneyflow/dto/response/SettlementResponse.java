package com.moneyflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 정산 결과 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {

    /**
     * 장부 ID
     */
    private UUID accountBookId;

    /**
     * 장부 이름
     */
    private String accountBookName;

    /**
     * 공용 지출 총액 (정산 대상)
     */
    private BigDecimal totalSharedExpense;

    /**
     * 개인 지출 총액 (정산 제외)
     */
    private BigDecimal totalPersonalExpense;

    /**
     * 멤버별 정산 내역
     */
    private List<MemberSettlement> members;

    /**
     * 정산 거래 내역 (누가 누구에게 얼마)
     */
    private List<SettlementTransaction> transactions;

    /**
     * 멤버별 정산 내역
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberSettlement {
        /**
         * 사용자 ID
         */
        private UUID userId;

        /**
         * 닉네임
         */
        private String nickname;

        /**
         * 본인이 결제한 공용 지출 금액
         */
        private BigDecimal paidAmount;

        /**
         * 본인이 부담해야 할 금액
         */
        private BigDecimal shouldPayAmount;

        /**
         * 차액 (+ 받을 돈, - 낼 돈)
         */
        private BigDecimal balance;
    }

    /**
     * 정산 거래 (송금 안내)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementTransaction {
        /**
         * 보내는 사람 ID
         */
        private UUID fromUserId;

        /**
         * 보내는 사람 닉네임
         */
        private String fromNickname;

        /**
         * 받는 사람 ID
         */
        private UUID toUserId;

        /**
         * 받는 사람 닉네임
         */
        private String toNickname;

        /**
         * 송금 금액
         */
        private BigDecimal amount;
    }
}
