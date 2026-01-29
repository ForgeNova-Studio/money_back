package com.moneyflow.domain.asset;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 자산 스냅샷 스케줄러
 * 매일 새벽 2시에 모든 가계부의 자산 총액을 스냅샷으로 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetSnapshotScheduler {

    private final AccountBookRepository accountBookRepository;
    private final AssetRepository assetRepository;
    private final AssetSnapshotRepository assetSnapshotRepository;

    /**
     * 매일 새벽 2시에 실행
     * 모든 가계부의 자산 총액을 스냅샷으로 저장
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void createDailySnapshots() {
        log.info("[AssetSnapshot] Starting daily snapshot job...");

        LocalDate today = LocalDate.now();
        List<AccountBook> accountBooks = accountBookRepository.findAll();

        int savedCount = 0;
        int skippedCount = 0;

        for (AccountBook accountBook : accountBooks) {
            try {
                // 이미 오늘 스냅샷이 있으면 스킵
                if (assetSnapshotRepository.existsByAccountBook_AccountBookIdAndSnapshotDate(
                        accountBook.getAccountBookId(), today)) {
                    skippedCount++;
                    continue;
                }

                // 가계부의 자산 총액 계산
                BigDecimal totalAmount = assetRepository.sumTotalAmountByAccountBook(
                        accountBook.getAccountBookId());

                // 자산 개수
                List<Asset> assets = assetRepository.findByAccountBookAccountBookIdOrderByCreatedAtDesc(
                        accountBook.getAccountBookId());

                // 스냅샷 저장
                AssetSnapshot snapshot = AssetSnapshot.builder()
                        .accountBook(accountBook)
                        .snapshotDate(today)
                        .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                        .assetCount(assets.size())
                        .build();

                assetSnapshotRepository.save(snapshot);
                savedCount++;

            } catch (Exception e) {
                log.error("[AssetSnapshot] Failed to create snapshot for accountBook: {}",
                        accountBook.getAccountBookId(), e);
            }
        }

        log.info("[AssetSnapshot] Daily snapshot completed. Saved: {}, Skipped: {}",
                savedCount, skippedCount);
    }

    /**
     * 90일 이상 된 오래된 스냅샷 삭제 (매주 월요일 3시)
     */
    @Scheduled(cron = "0 0 3 * * MON")
    @Transactional
    public void cleanupOldSnapshots() {
        log.info("[AssetSnapshot] Starting cleanup of old snapshots...");

        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        assetSnapshotRepository.deleteOlderThan(cutoffDate);

        log.info("[AssetSnapshot] Cleanup completed. Deleted snapshots older than: {}", cutoffDate);
    }
}
