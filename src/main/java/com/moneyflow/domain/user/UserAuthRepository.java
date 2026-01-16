package com.moneyflow.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAuthRepository extends JpaRepository<UserAuth, UUID> {

    /**
     * 이메일과 provider로 인증 정보 조회 (이메일 로그인용)
     */
    @Query("SELECT ua FROM UserAuth ua JOIN FETCH ua.user WHERE ua.user.email = :email AND ua.provider = :provider")
    Optional<UserAuth> findByUserEmailAndProvider(String email, AuthProvider provider);

    /**
     * provider와 providerId로 인증 정보 조회 (소셜 로그인용)
     */
    @Query("SELECT ua FROM UserAuth ua JOIN FETCH ua.user WHERE ua.provider = :provider AND ua.providerId = :providerId")
    Optional<UserAuth> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /**
     * 사용자의 모든 인증 수단 조회
     */
    List<UserAuth> findByUser(User user);

    /**
     * 사용자 ID와 provider로 인증 정보 조회
     */
    Optional<UserAuth> findByUserUserIdAndProvider(UUID userId, AuthProvider provider);
}
