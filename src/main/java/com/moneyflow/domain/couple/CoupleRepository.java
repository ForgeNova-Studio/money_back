package com.moneyflow.domain.couple;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 커플 리포지토리
 */
@Repository
public interface CoupleRepository extends JpaRepository<Couple, UUID> {

       /**
        * 초대 코드로 커플 조회 (User1, User2 JOIN FETCH)
        * N+1 방지: toResponse()에서 user1, user2 접근 시 추가 쿼리 방지
        */
       @Query("SELECT c FROM Couple c " +
                     "LEFT JOIN FETCH c.user1 " +
                     "LEFT JOIN FETCH c.user2 " +
                     "WHERE c.inviteCode = :inviteCode")
       Optional<Couple> findByInviteCode(@Param("inviteCode") String inviteCode);

       /**
        * 커플 ID로 조회 (User1, User2 JOIN FETCH)
        */
       @Query("SELECT c FROM Couple c " +
                     "LEFT JOIN FETCH c.user1 " +
                     "LEFT JOIN FETCH c.user2 " +
                     "WHERE c.coupleId = :coupleId")
       Optional<Couple> findByIdWithUsers(@Param("coupleId") UUID coupleId);

       /**
        * 사용자가 속한 커플 조회 (User1, User2 JOIN FETCH)
        * user1 또는 user2로 등록된 커플을 찾습니다.
        */
       @Query("SELECT c FROM Couple c " +
                     "LEFT JOIN FETCH c.user1 " +
                     "LEFT JOIN FETCH c.user2 " +
                     "WHERE c.user1.userId = :userId OR c.user2.userId = :userId")
       Optional<Couple> findByUserId(@Param("userId") UUID userId);

       /**
        * 사용자가 이미 커플에 속해있는지 확인
        */
       @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
                     "FROM Couple c WHERE c.user1.userId = :userId OR c.user2.userId = :userId")
       boolean existsByUserId(@Param("userId") UUID userId);

       /**
        * 연동이 완료된 커플만 조회 (User1, User2 JOIN FETCH)
        */
       @Query("SELECT c FROM Couple c " +
                     "LEFT JOIN FETCH c.user1 " +
                     "LEFT JOIN FETCH c.user2 " +
                     "WHERE (c.user1.userId = :userId OR c.user2.userId = :userId) " +
                     "AND c.linkedAt IS NOT NULL")
       Optional<Couple> findLinkedCoupleByUserId(@Param("userId") UUID userId);
}
