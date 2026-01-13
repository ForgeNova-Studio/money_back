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
     * 사용자가 참여 중인 장부 목록 조회
     */
    @Query("SELECT ab FROM AccountBook ab " +
            "JOIN ab.members m " +
            "WHERE m.user.userId = :userId AND ab.isActive = true " +
            "ORDER BY ab.createdAt DESC")
    List<AccountBook> findByMemberUserId(@Param("userId") UUID userId);

    /**
     * 사용자가 참여 중인 활성 장부 목록 (유형별)
     */
    @Query("SELECT ab FROM AccountBook ab " +
            "JOIN ab.members m " +
            "WHERE m.user.userId = :userId AND ab.isActive = true AND ab.bookType = :bookType " +
            "ORDER BY ab.createdAt DESC")
    List<AccountBook> findByMemberUserIdAndBookType(
            @Param("userId") UUID userId,
            @Param("bookType") BookType bookType);

    /**
     * 커플의 장부 목록 조회
     */
    List<AccountBook> findByCouple_CoupleIdAndIsActiveTrue(UUID coupleId);

    /**
     * 커플의 특정 유형 장부 조회
     */
    Optional<AccountBook> findByCouple_CoupleIdAndBookTypeAndIsActiveTrue(UUID coupleId, BookType bookType);

    /**
     * 커플 ID로 장부 존재 확인
     */
    boolean existsByCouple_CoupleIdAndBookType(UUID coupleId, BookType bookType);

    /**
     * 장부 ID와 멤버 확인 (권한 체크용)
     */
    @Query("SELECT ab FROM AccountBook ab " +
            "JOIN ab.members m " +
            "WHERE ab.accountBookId = :accountBookId AND m.user.userId = :userId")
    Optional<AccountBook> findByIdAndMemberUserId(
            @Param("accountBookId") UUID accountBookId,
            @Param("userId") UUID userId);
}
