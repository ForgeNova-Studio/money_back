package com.moneyflow.service;

import com.moneyflow.domain.token.RefreshToken;
import com.moneyflow.domain.token.RefreshTokenRepository;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.response.LoginResponse;
import com.moneyflow.dto.response.RegisterResponse;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import com.moneyflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public RegisterResponse issueRegisterResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        save(refreshToken, user);

        return RegisterResponse.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public LoginResponse issueLoginResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        save(refreshToken, user);
        return buildLoginResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public void save(String refreshToken, User user) {
        String tokenHash = hashToken(refreshToken);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);
        log.debug("Refresh Token 저장: 사용자 {} ({})", user.getEmail(), user.getUserId());
    }

    @Transactional
    public LoginResponse rotate(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw UnauthorizedException.authentication("유효하지 않거나 만료된 Refresh Token입니다");
        }

        String tokenHash = hashToken(refreshToken);
        RefreshToken refreshTokenEntity = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 Refresh Token입니다"));

        if (refreshTokenEntity.isExpired()) {
            throw UnauthorizedException.authentication("만료된 Refresh Token입니다");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        log.info("토큰 갱신 (Rotation): {} ({})", user.getEmail(), user.getUserId());

        refreshTokenEntity.revoke();
        refreshTokenRepository.save(refreshTokenEntity);
        log.debug("기존 Refresh Token 무효화: {}", tokenHash.substring(0, Math.min(10, tokenHash.length())) + "...");

        return issueLoginResponse(user);
    }

    @Transactional
    public void revoke(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        var tokenOptional = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOptional.isPresent()) {
            RefreshToken refreshTokenEntity = tokenOptional.get();
            refreshTokenEntity.revoke();
            refreshTokenRepository.save(refreshTokenEntity);

            log.info("로그아웃: 사용자 {} ({})",
                    refreshTokenEntity.getUser().getEmail(),
                    refreshTokenEntity.getUser().getUserId());
            return;
        }

        log.warn("로그아웃 요청: 유효하지 않거나 이미 삭제된 Refresh Token (해시: {})",
                tokenHash.substring(0, Math.min(10, tokenHash.length())) + "...");
    }

    String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        LoginResponse.UserProfile profile = LoginResponse.UserProfile.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImage(user.getProfileImageUrl())
                .build();

        return LoginResponse.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .profile(profile)
                .build();
    }
}
