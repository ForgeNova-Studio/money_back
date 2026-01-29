package com.moneyflow.domain.asset;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.AssetRequest;
import com.moneyflow.dto.response.AssetResponse;
import com.moneyflow.dto.response.AssetSummaryResponse;
import com.moneyflow.dto.response.AssetSummaryResponse.CategoryBreakdownDto;
import com.moneyflow.dto.response.AssetSummaryResponse.GroupBreakdownDto;
import com.moneyflow.domain.asset.AssetCategory.AssetGroup;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetSnapshotRepository assetSnapshotRepository;
    private final UserRepository userRepository;
    private final AccountBookRepository accountBookRepository;

    /**
     * 자산 생성
     */
    @Transactional
    public AssetResponse createAsset(UUID userId, AssetRequest request) {
        User user = findUser(userId);

        AccountBook accountBook = null;
        if (request.getAccountBookId() != null) {
            accountBook = findAccountBook(request.getAccountBookId(), userId);
        }

        Asset asset = Asset.builder()
                .user(user)
                .accountBook(accountBook)
                .name(request.getName())
                .category(request.getCategory())
                .amount(request.getAmount())
                .memo(request.getMemo())
                .build();

        Asset saved = assetRepository.save(asset);
        log.info("자산 생성 완료: userId={}, assetId={}, name={}", userId, saved.getAssetId(), saved.getName());

        return AssetResponse.from(saved);
    }

    /**
     * 자산 목록 조회
     */
    public List<AssetResponse> getAssets(UUID userId) {
        return assetRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AssetResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 자산 조회
     */
    public List<AssetResponse> getAssetsByCategory(UUID userId, AssetCategory category) {
        return assetRepository.findByUserUserIdAndCategoryOrderByCreatedAtDesc(userId, category)
                .stream()
                .map(AssetResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 자산 상세 조회
     */
    public AssetResponse getAsset(UUID userId, UUID assetId) {
        Asset asset = findAsset(assetId, userId);
        return AssetResponse.from(asset);
    }

    /**
     * 자산 수정
     */
    @Transactional
    public AssetResponse updateAsset(UUID userId, UUID assetId, AssetRequest request) {
        Asset asset = findAsset(assetId, userId);

        if (request.getAccountBookId() != null) {
            AccountBook accountBook = findAccountBook(request.getAccountBookId(), userId);
            asset.setAccountBook(accountBook);
        }

        asset.setName(request.getName());
        asset.setCategory(request.getCategory());
        asset.setAmount(request.getAmount());
        asset.setMemo(request.getMemo());

        log.info("자산 수정 완료: userId={}, assetId={}", userId, assetId);
        return AssetResponse.from(asset);
    }

    /**
     * 자산 삭제
     */
    @Transactional
    public void deleteAsset(UUID userId, UUID assetId) {
        Asset asset = findAsset(assetId, userId);
        assetRepository.delete(asset);
        log.info("자산 삭제 완료: userId={}, assetId={}", userId, assetId);
    }

    /**
     * 자산 요약 조회 (전월 대비 포함, 2단계 그룹핑)
     */
    public AssetSummaryResponse getAssetSummary(UUID userId) {
        // 현재 자산 목록
        List<Asset> assets = assetRepository.findByUserUserIdOrderByCreatedAtDesc(userId);

        // 총 자산
        BigDecimal totalAmount = assetRepository.sumTotalAmount(userId);

        // 카테고리별 집계
        List<Object[]> categoryStats = assetRepository.sumAmountByCategory(userId);
        List<CategoryBreakdownDto> categoryBreakdowns = buildCategoryBreakdowns(categoryStats, totalAmount);

        // 그룹별 집계 (Level 1)
        List<GroupBreakdownDto> groupBreakdowns = buildGroupBreakdowns(categoryBreakdowns, totalAmount);

        // 전월 대비 변화 계산
        BigDecimal previousMonthDiff = calculatePreviousMonthDiff(assets);

        return AssetSummaryResponse.builder()
                .totalAmount(totalAmount)
                .previousMonthDiff(previousMonthDiff)
                .groupBreakdowns(groupBreakdowns)
                .categoryBreakdowns(categoryBreakdowns)
                .assets(assets.stream().map(AssetResponse::from).collect(Collectors.toList()))
                .build();
    }

    // ===== Private Helper Methods =====

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Asset findAsset(UUID assetId, UUID userId) {
        return assetRepository.findByAssetIdAndUserUserId(assetId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
    }

    private AccountBook findAccountBook(UUID accountBookId, UUID userId) {
        return accountBookRepository.findByIdAndMemberUserId(accountBookId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_BOOK_NOT_FOUND));
    }

    private List<CategoryBreakdownDto> buildCategoryBreakdowns(List<Object[]> stats, BigDecimal total) {
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyList();
        }

        return stats.stream()
                .map(row -> {
                    AssetCategory category = (AssetCategory) row[0];
                    BigDecimal amount = (BigDecimal) row[1];
                    double percent = calculatePercent(amount, total);

                    return CategoryBreakdownDto.builder()
                            .category(category)
                            .categoryLabel(category.getLabel())
                            .group(category.getGroup())
                            .groupLabel(category.getGroup().getLabel())
                            .amount(amount)
                            .percent(percent)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    /**
     * 그룹별 집계 (Level 1)
     * 카테고리 breakdown을 그룹별로 묶어서 반환
     */
    private List<GroupBreakdownDto> buildGroupBreakdowns(List<CategoryBreakdownDto> categoryBreakdowns,
            BigDecimal total) {
        if (categoryBreakdowns == null || categoryBreakdowns.isEmpty()) {
            return Collections.emptyList();
        }

        // 그룹별로 카테고리 묶기
        Map<AssetGroup, List<CategoryBreakdownDto>> groupedCategories = categoryBreakdowns.stream()
                .collect(Collectors.groupingBy(CategoryBreakdownDto::getGroup));

        // 그룹별 합계 계산
        return Arrays.stream(AssetGroup.values())
                .map(group -> {
                    List<CategoryBreakdownDto> categories = groupedCategories.getOrDefault(group,
                            Collections.emptyList());

                    BigDecimal groupAmount = categories.stream()
                            .map(CategoryBreakdownDto::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    double percent = calculatePercent(groupAmount, total);

                    return GroupBreakdownDto.builder()
                            .group(group)
                            .groupLabel(group.getLabel())
                            .amount(groupAmount)
                            .percent(percent)
                            .categories(categories)
                            .build();
                })
                .filter(g -> g.getAmount().compareTo(BigDecimal.ZERO) > 0) // 금액이 0인 그룹 제외
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    private double calculatePercent(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return amount.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 전월 대비 변화 계산
     * 지난달 말 스냅샷과 현재 총자산을 비교
     * 스냅샷이 없으면 이번 달 생성된 자산 금액으로 폴백
     */
    private BigDecimal calculatePreviousMonthDiff(List<Asset> assets) {
        BigDecimal currentTotal = assets.stream()
                .map(Asset::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 지난달 말일
        LocalDate lastDayOfPreviousMonth = YearMonth.now().minusMonths(1).atEndOfMonth();

        // 가계부별로 스냅샷 조회 (첫 번째 자산의 가계부 기준)
        // TODO: 사용자별 총자산 스냅샷으로 개선 필요
        if (!assets.isEmpty() && assets.get(0).getAccountBook() != null) {
            UUID accountBookId = assets.get(0).getAccountBook().getAccountBookId();
            Optional<AssetSnapshot> snapshot = assetSnapshotRepository
                    .findLatestBeforeDate(accountBookId, lastDayOfPreviousMonth);

            if (snapshot.isPresent()) {
                BigDecimal previousTotal = snapshot.get().getTotalAmount();
                return currentTotal.subtract(previousTotal);
            }
        }

        // 스냅샷이 없으면 기존 로직 (이번 달 생성된 자산)
        LocalDate firstDayOfMonth = YearMonth.now().atDay(1);
        return assets.stream()
                .filter(a -> a.getCreatedAt() != null &&
                        a.getCreatedAt().toLocalDate().isAfter(firstDayOfMonth.minusDays(1)))
                .map(Asset::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
