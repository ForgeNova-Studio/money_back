package com.moneyflow.domain.income;

import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 수입 엔티티
 *
 * 사용자의 수입 정보를 관리합니다.
 * - 급여, 부수입, 상여금 등 다양한 수입 출처 지원
 * - 개인/커플 모드 지원
 */
@Entity
@Table(name = "incomes", indexes = {
        @Index(name = "idx_income_user_date", columnList = "user_id, date DESC"),
        @Index(name = "idx_income_couple_date", columnList = "couple_id, date DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Income {

    /**
     * 수입 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "income_id")
    private UUID incomeId;

    /**
     * 수입을 등록한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 커플 ID (커플 모드인 경우)
     * NULL이면 개인 수입
     */
    @Column(name = "couple_id")
    private UUID coupleId;

    /**
     * 수입 금액
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * 수입 날짜
     */
    @Column(nullable = false)
    private LocalDate date;

    /**
     * 수입 출처
     * 예: 급여, 부수입, 상여금, 투자수익, 기타
     */
    @Column(nullable = false, length = 50)
    private String source;

    /**
     * 수입 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
