package com.moneyflow.domain.accountbook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 장부 리포지토리
 */
@Repository
public interface AccountBookRepository extends JpaRepository<AccountBook, UUID> {

        /**
         * 장부 ID로 조회 (Members, User JOIN FETCH)
         * N+1 방지: toResponse()에서 members, members.user 접근 시 추가 쿼리 방지
         */
        @Query("SELECT DISTINCT ab FROM AccountBook ab " +
                        "LEFT JOIN FETCH ab.members m " +
                        "LEFT JOIN FETCH m.user " +
                        "LEFT JOIN FETCH ab.couple " +
                        "WHERE ab.accountBookId = :accountBookId")
        Optional<AccountBook> findByIdWithMembersAndUsers(@Param("accountBookId") UUID accountBookId);

        /**
         * 사용자가 참여 중인 장부 목록 조회 (Members, User JOIN FETCH)
         * N+1 방지: 리스트 순회 시 members, members.user 접근
         */
        @Query("SELECT DISTINCT ab FROM AccountBook ab " +
                        "LEFT JOIN FETCH ab.members m " +
                        "LEFT JOIN FETCH m.user " +
                        "LEFT JOIN FETCH ab.couple " +
                        "WHERE ab.accountBookId IN (" +
                        "  SELECT ab2.accountBookId FROM AccountBook ab2 " +
                        "  JOIN ab2.members m2 " +
                        "  WHERE m2.user.userId = :userId AND ab2.isActive = true" +
                        ") " +
                        "ORDER BY ab.createdAt DESC")
        List<AccountBook> findByMemberUserId(@Param("userId") UUID userId);

        /**
         * 사용자가 참여 중인 활성 장부 목록 (유형별, Members JOIN FETCH)
         */
        @Query("SELECT DISTINCT ab FROM AccountBook ab " +
                        "LEFT JOIN FETCH ab.members m " +
                        "LEFT JOIN FETCH m.user " +
                        "WHERE ab.accountBookId IN (" +
                        "  SELECT ab2.accountBookId FROM AccountBook ab2 " +
                        "  JOIN ab2.members m2 " +
                        "  WHERE m2.user.userId = :userId AND ab2.isActive = true AND ab2.bookType = :bookType" +
                        ") " +
                        "ORDER BY ab.createdAt DESC")
        List<AccountBook> findByMemberUserIdAndBookType(
                        @Param("userId") UUID userId,
                        @Param("bookType") BookType bookType);

        /**
         * 커플의 장부 목록 조회 (Members JOIN FETCH)
         */
        @Query("SELECT DISTINCT ab FROM AccountBook ab " +
                        "LEFT JOIN FETCH ab.members m " +
                        "LEFT JOIN FETCH m.user " +
                        "WHERE ab.couple.coupleId = :coupleId AND ab.isActive = true")
        List<AccountBook> findByCouple_CoupleIdAndIsActiveTrue(@Param("coupleId") UUID coupleId);

        /**
         * 커플의 특정 유형 장부 조회 (Members JOIN FETCH)
         */
        @Query("SELECT DISTINCT ab FROM AccountBook ab " +
                        "LEFT JOIN FETCH ab.members m " +
                        "LEFT JOIN FETCH m.user " +
                        "WHERE ab.couple.coupleId = :coupleId AND ab.bookType = :bookType AND ab.isActive = true")
        Optional<AccountBook> findByCouple_CoupleIdAndBookTypeAndIsActiveTrue(
                        @Param("coupleId") UUID coupleId,
                        @Param("bookType") BookType bookType);

        /**
         * 커플 ID로 장부 존재 확인
         */
        boolean existsByCouple_CoupleIdAndBookType(UUID coupleId, BookType bookType);

        /**
         * 사용자별 특정 유형 장부 존재 확인
         */
        @Query("SELECT COUNT(ab) > 0 FROM AccountBook ab " +
                        "JOIN ab.members m " +
                        "WHERE m.user.userId = :userId AND ab.bookType = :bookType")
        boolean existsByMemberUserIdAndBookType(
                        @Param("userId") UUID userId,
                        @Param("bookType") BookType bookType);

        /**
         * 사용자의 기본 장부 조회 (DEFAULT 타입, Members JOIN FETCH)
         */
        @Query("SELECT DISTINCT ab FROM AccountBook ab " +
                        "LEFT JOIN FETCH ab.members m " +
                        "LEFT JOIN FETCH m.user " +
                        "WHERE ab.accountBookId IN (" +
                        "  SELECT ab2.accountBookId FROM AccountBook ab2 " +
                        "  JOIN ab2.members m2 " +
                        "  WHERE m2.user.userId = :userId AND ab2.bookType = 'DEFAULT' AND ab2.isActive = true" +
                        ") " +
                        "ORDER BY ab.createdAt ASC " +
                        "LIMIT 1")
        Optional<AccountBook> findDefaultAccountBookByUserId(@Param("userId") UUID userId);

        /**
         * 장부 ID와 멤버 확인 (권한 체크용, Members JOIN FETCH)
         */
        @Query("SELECT DISTINCT ab FROM AccountBook ab " +
                        "LEFT JOIN FETCH ab.members m " +
                        "LEFT JOIN FETCH m.user " +
                        "WHERE ab.accountBookId = :accountBookId " +
                        "AND ab.accountBookId IN (" +
                        "  SELECT ab2.accountBookId FROM AccountBook ab2 " +
                        "  JOIN ab2.members m2 " +
                        "  WHERE m2.user.userId = :userId" +
                        ")")
        Optional<AccountBook> findByIdAndMemberUserId(
                        @Param("accountBookId") UUID accountBookId,
                        @Param("userId") UUID userId);
}
