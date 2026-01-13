package com.moneyflow.domain.accountbook;

/**
 * 지출 출처 (공금 vs 개인)
 */
public enum FundingSource {
    PERSONAL, // 내 지갑에서 지출
    SHARED_POOL // 공금에서 지출
}
