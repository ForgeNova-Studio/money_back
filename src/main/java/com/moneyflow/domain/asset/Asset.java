package com.moneyflow.domain.asset;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 개별 자산 엔티티
 * 사용자의 자산(현금, 적금, 주식, 부동산 등)을 개별적으로 관리
 */
@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "asset_id")
    private UUID assetId;

    /**
     * 자산 소유자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 소속 가계부 (선택)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_book_id")
    private AccountBook accountBook;

    /**
     * 자산명 (예: "비상금 통장", "삼성전자", "청약저축")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 자산 카테고리
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetCategory category;

    /**
     * 현재 금액
     */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /**
     * 메모 (선택)
     */
    @Column(columnDefinition = "TEXT")
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
