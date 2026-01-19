package com.moneyflow.domain.recurringexpense;

/**
 * 월별 결제 상태
 */
public enum PaymentStatus {
    /**
     * 미확정 (예상 금액으로 표시)
     */
    PENDING,

    /**
     * 확정됨 (실제 지출과 연결됨)
     */
    CONFIRMED,

    /**
     * 이번 달 건너뜀
     */
    SKIPPED
}
