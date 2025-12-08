package com.moneyflow.domain.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token Repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * 토큰 해시로 조회
     *
     * @param tokenHash SHA-256 해시값
     * @return RefreshToken
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 무효화되지 않은 토큰을 해시로 조회
     *
     * 가장 빈번하게 사용되는 쿼리 (refresh 시)
     *
     * @param tokenHash SHA-256 해시값
     * @return 유효한 RefreshToken
     */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /**
     * 사용자의 모든 토큰 조회
     *
     * @param userId 사용자 ID
     * @return RefreshToken 리스트
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.userId = :userId ORDER BY rt.createdAt DESC")
    List<RefreshToken> findByUserId(@Param("userId") UUID userId);

    /**
     * 사용자의 유효한 토큰만 조회
     *
     * @param userId 사용자 ID
     * @return 유효한 RefreshToken 리스트
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.userId = :userId AND rt.revoked = false ORDER BY rt.createdAt DESC")
    List<RefreshToken> findValidTokensByUserId(@Param("userId") UUID userId);

    /**
     * 사용자의 모든 토큰 무효화 (전체 로그아웃)
     *
     * @param userId 사용자 ID
     * @return 무효화된 토큰 개수
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.userId = :userId AND rt.revoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId);

    /**
     * 만료된 토큰 삭제 (정리 작업용)
     *
     * @param now 현재 시간
     * @return 삭제된 토큰 개수
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 사용자가 가진 토큰 개수 조회
     *
     * @param userId 사용자 ID
     * @return 토큰 개수
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.userId = :userId AND rt.revoked = false")
    long countValidTokensByUserId(@Param("userId") UUID userId);
}
