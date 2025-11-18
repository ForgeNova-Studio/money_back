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
     * 초대 코드로 커플 조회
     *
     * @param inviteCode 초대 코드
     * @return 커플 정보 (Optional)
     */
    Optional<Couple> findByInviteCode(String inviteCode);

    /**
     * 사용자가 속한 커플 조회
     * user1 또는 user2로 등록된 커플을 찾습니다.
     *
     * @param userId 사용자 ID
     * @return 커플 정보 (Optional)
     */
    @Query("SELECT c FROM Couple c WHERE c.user1.userId = :userId OR c.user2.userId = :userId")
    Optional<Couple> findByUserId(@Param("userId") UUID userId);

    /**
     * 사용자가 이미 커플에 속해있는지 확인
     *
     * @param userId 사용자 ID
     * @return 커플에 속해있으면 true
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM Couple c WHERE c.user1.userId = :userId OR c.user2.userId = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);

    /**
     * 연동이 완료된 커플만 조회
     *
     * @param userId 사용자 ID
     * @return 연동 완료된 커플 정보 (Optional)
     */
    @Query("SELECT c FROM Couple c WHERE (c.user1.userId = :userId OR c.user2.userId = :userId) " +
           "AND c.linkedAt IS NOT NULL")
    Optional<Couple> findLinkedCoupleByUserId(@Param("userId") UUID userId);
}
