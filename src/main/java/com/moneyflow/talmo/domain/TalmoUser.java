package com.moneyflow.talmo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "talmo_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TalmoUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "kakao_access_token", length = 500)
    private String kakaoAccessToken;

    @Column(name = "kakao_refresh_token", length = 500)
    private String kakaoRefreshToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public TalmoUser(String name) {
        this.name = name;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateKakaoTokens(String accessToken, String refreshToken) {
        this.kakaoAccessToken = accessToken;
        this.kakaoRefreshToken = refreshToken;
    }

    public boolean hasKakaoToken() {
        return this.kakaoRefreshToken != null && !this.kakaoRefreshToken.isEmpty();
    }
}
