package com.moneyflow.talmo.repository;

import com.moneyflow.talmo.domain.TalmoUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TalmoUserRepository extends JpaRepository<TalmoUser, Long> {
    Optional<TalmoUser> findByName(String name);

    boolean existsByName(String name);

    List<TalmoUser> findByKakaoRefreshTokenIsNotNull();
}
