package com.moneyflow.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 거래 내역 검색 응답 DTO
 * - 지출(Expense)과 수입(Income)을 통합하여 반환
 * - 전체 기간 검색, 날짜 역순 정렬, 페이지네이션 지원
 */
@Getter
@Builder
public class SearchResponse {
    private List<TransactionDto> transactions;
    private int totalCount;
    private boolean hasNext;
}
