package com.moneyflow.domain.accountbook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 장부 멤버 리포지토리
 */
@Repository
public interface AccountBookMemberRepository extends JpaRepository<AccountBookMember, AccountBookMemberId> {

    /**
     * 장부의 모든 멤버 조회 (User JOIN FETCH)
     * N+1 방지: getMembers()에서 member.getUser() 접근 시 추가 쿼리 방지
     */
    @Query("SELECT m FROM AccountBookMember m " +
            "JOIN FETCH m.user " +
            "JOIN FETCH m.accountBook " +
            "WHERE m.accountBook.accountBookId = :accountBookId")
    List<AccountBookMember> findByAccountBookAccountBookId(@Param("accountBookId") UUID accountBookId);

    /**
     * 사용자가 참여 중인 모든 멤버십 조회 (AccountBook JOIN FETCH)
     */
    @Query("SELECT m FROM AccountBookMember m " +
            "JOIN FETCH m.user " +
            "JOIN FETCH m.accountBook " +
            "WHERE m.user.userId = :userId")
    List<AccountBookMember> findByUserUserId(@Param("userId") UUID userId);

    /**
     * 특정 장부에서 사용자가 멤버인지 확인
     */
    boolean existsByAccountBookAccountBookIdAndUserUserId(UUID accountBookId, UUID userId);

    /**
     * 장부의 멤버 수 조회
     */
    @Query("SELECT COUNT(m) FROM AccountBookMember m WHERE m.accountBook.accountBookId = :accountBookId")
    long countByAccountBookId(@Param("accountBookId") UUID accountBookId);
}
