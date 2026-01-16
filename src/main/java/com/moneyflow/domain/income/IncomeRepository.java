package com.moneyflow.domain.income;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 수입 레포지토리
 *
 * 수입 데이터에 대한 데이터베이스 액세스를 제공합니다.
 */
@Repository
public interface IncomeRepository extends JpaRepository<Income, UUID> {

        /**
         * 사용자별 기간별 수입 조회
         *
         * @param userId    사용자 ID
         * @param startDate 시작 날짜
         * @param endDate   종료 날짜
         * @param source    수입 출처 (null이면 전체 조회)
         * @return 조회된 수입 목록
         */
        @Query("SELECT i FROM Income i WHERE i.user.userId = :userId " +
                        "AND i.date BETWEEN :startDate AND :endDate " +
                        "AND (:source IS NULL OR i.source = :source) " +
                        "ORDER BY i.date DESC, i.createdAt DESC")
        List<Income> findIncomesByUserAndDateRange(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("source") String source);

        /**
         * 장부별 기간별 수입 조회
         */
        @Query("SELECT i FROM Income i WHERE i.accountBook.accountBookId = :accountBookId " +
                        "AND i.date BETWEEN :startDate AND :endDate " +
                        "AND (:source IS NULL OR i.source = :source) " +
                        "ORDER BY i.date DESC, i.createdAt DESC")
        List<Income> findByAccountBookAndDateRange(
                        @Param("accountBookId") UUID accountBookId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("source") String source);

        /**
         * 장부별 모든 수입 조회
         */
        List<Income> findByAccountBook_AccountBookIdOrderByDateDescCreatedAtDesc(UUID accountBookId);

        /**
         * 사용자별 최근 5개 수입 조회
         *
         * @param userId 사용자 ID
         * @return 최근 수입 목록 (최대 5개)
         */
        List<Income> findTop5ByUserUserIdOrderByDateDescCreatedAtDesc(UUID userId);

        /**
         * 특정 수입이 사용자의 것인지 확인
         *
         * @param incomeId 수입 ID
         * @param userId   사용자 ID
         * @return 사용자의 수입이면 true
         */
        boolean existsByIncomeIdAndUserUserId(UUID incomeId, UUID userId);

        /**
         * 기간별 모든 수입 조회 (홈 화면 월간 데이터용)
         *
         * @param startDate 시작 날짜
         * @param endDate   종료 날짜
         * @return 조회된 수입 목록
         */
        List<Income> findByDateBetween(LocalDate startDate, LocalDate endDate);

        /**
         * 사용자별 기간 수입 조회 (홈 화면 월간 데이터용)
         *
         * @param userId    사용자 ID
         * @param startDate 시작 날짜
         * @param endDate   종료 날짜
         * @return 조회된 수입 목록
         */
        List<Income> findByUser_UserIdAndDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);
}
