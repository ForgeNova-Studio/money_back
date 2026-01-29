package com.moneyflow.domain.asset;

import com.moneyflow.domain.accountbook.AccountBook;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 자산 스냅샷 엔티티
 * 매일 새벽에 가계부별 자산 총액을 저장하여 월간 변화 추적
 */
@Entity
@Table(name = "asset_snapshots", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account_book_id", "snapshot_date" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "snapshot_id")
    private UUID snapshotId;

    /**
     * 대상 가계부
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_book_id", nullable = false)
    private AccountBook accountBook;

    /**
     * 스냅샷 날짜 (해당 날짜 기준 자산 총액)
     */
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    /**
     * 해당 날짜의 자산 총액
     */
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 자산 개수
     */
    @Column(name = "asset_count")
    private Integer assetCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
