package com.moneyflow.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 홈 화면 월간 데이터 조회용 거래 DTO
 * - 지출(Expense)과 수입(Income)을 통합하여 표현
 * - UUID를 String으로 변환하여 전달 (프론트엔드 호환성)
 */
@Getter
@Builder
public class TransactionDto {
    private String id;       // UUID를 String으로 변환 (예: "123e4567-e89b-12d3-a456-426614174000")
    private String type;     // "INCOME" or "EXPENSE"
    private long amount;
    private String title;    // 사용자 입력 설명 (지출: merchant ?? category, 수입: description ?? source)
    private String category; // 카테고리 코드 (지출: category, 수입: source)
    private String memo;     // 추가 메모 (지출: memo, 수입: null)
    private String time;     // "14:30" (현재는 빈 문자열, 향후 확장 가능)
}
