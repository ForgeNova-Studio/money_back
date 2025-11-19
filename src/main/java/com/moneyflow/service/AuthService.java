package com.moneyflow.service;

import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.LoginRequest;
import com.moneyflow.dto.request.RegisterRequest;
import com.moneyflow.dto.request.SocialLoginRequest;
import com.moneyflow.dto.response.LoginResponse;
import com.moneyflow.dto.response.RegisterResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final GoogleOAuthService googleOAuthService;
    private final AppleOAuthService appleOAuthService;
    private final NaverOAuthService naverOAuthService;
    private final KakaoOAuthService kakaoOAuthService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("이미 사용 중인 이메일입니다");
        }

        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        user = userRepository.save(user);

        log.info("새로운 사용자 등록: {}", user.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return RegisterResponse.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            // 인증 시도
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // 사용자 조회
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다"));

            log.info("사용자 로그인: {}", user.getEmail());

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // 프로필 정보 생성
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

        } catch (BadCredentialsException e) {
            log.warn("로그인 실패: {}", request.getEmail());
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
    }

    /**
     * 소셜 로그인 처리
     * 1. ID Token 검증
     * 2. 기존 사용자 조회 (provider + providerId)
     * 3. 없으면 자동 회원가입, 있으면 로그인
     */
    @Transactional
    public LoginResponse socialLogin(SocialLoginRequest request) {
        try {
            String email;
            String providerId;
            String name = request.getNickname();

            // Provider별 토큰 검증
            if (request.getProvider() == AuthProvider.GOOGLE) {
                GoogleOAuthService.GoogleUserInfo userInfo = googleOAuthService.verifyIdToken(request.getIdToken());
                email = userInfo.email();
                providerId = userInfo.providerId();
                if (name == null) {
                    name = userInfo.name();
                }
            } else if (request.getProvider() == AuthProvider.APPLE) {
                AppleOAuthService.AppleUserInfo userInfo = appleOAuthService.verifyIdToken(request.getIdToken());
                email = userInfo.email();
                providerId = userInfo.providerId();
            } else if (request.getProvider() == AuthProvider.NAVER) {
                NaverOAuthService.NaverUserInfo userInfo = naverOAuthService.verifyAccessToken(request.getIdToken());
                email = userInfo.email();
                providerId = userInfo.providerId();
                if (name == null) {
                    name = userInfo.name();
                }
            } else if (request.getProvider() == AuthProvider.KAKAO) {
                KakaoOAuthService.KakaoUserInfo userInfo = kakaoOAuthService.verifyAccessToken(request.getIdToken());
                email = userInfo.email();
                providerId = userInfo.providerId();
                if (name == null) {
                    name = userInfo.nickname();
                }
            } else {
                throw new BusinessException("지원하지 않는 로그인 제공자입니다");
            }

            // 기존 사용자 조회 (provider + providerId)
            User user = userRepository.findByProviderAndProviderId(request.getProvider(), providerId)
                    .orElse(null);

            // 기존 사용자 없음 → 자동 회원가입
            if (user == null) {
                // 이메일로 다른 provider의 기존 사용자가 있는지 확인
                if (userRepository.existsByEmail(email)) {
                    throw new BusinessException("이미 다른 방법으로 가입된 이메일입니다");
                }

                // 닉네임이 없으면 이메일 앞부분 사용
                if (name == null || name.isBlank()) {
                    name = email.split("@")[0];
                }

                // 자동 회원가입
                user = User.builder()
                        .email(email)
                        .nickname(name)
                        .provider(request.getProvider())
                        .providerId(providerId)
                        .passwordHash(null)  // 소셜 로그인은 비밀번호 없음
                        .build();

                user = userRepository.save(user);
                log.info("소셜 로그인으로 새로운 사용자 등록: {} ({})", email, request.getProvider());
            } else {
                log.info("소셜 로그인: {} ({})", email, request.getProvider());
            }

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // 프로필 정보 생성
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

        } catch (GeneralSecurityException | IOException e) {
            log.error("소셜 로그인 토큰 검증 실패", e);
            throw new BusinessException("유효하지 않은 소셜 로그인 토큰입니다");
        } catch (IllegalArgumentException e) {
            log.error("소셜 로그인 토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException("유효하지 않은 소셜 로그인 토큰입니다");
        }
    }

    /**
     * Mock 소셜 로그인 (개발/테스트 전용)
     * ID Token 검증 없이 소셜 로그인을 테스트할 수 있습니다.
     * ⚠️ 프로덕션 환경에서는 비활성화되어야 합니다.
     */
    @Transactional
    public LoginResponse mockSocialLogin(SocialLoginRequest request) {
        log.warn("⚠️ Mock 소셜 로그인 사용 중 - 개발/테스트 전용");

        // Mock 사용자 정보 생성
        String email = "mock_" + request.getProvider().name().toLowerCase() + "_" +
                       System.currentTimeMillis() + "@test.com";
        String providerId = "MOCK_" + System.currentTimeMillis();
        String name = request.getNickname() != null ? request.getNickname() : "Mock User";

        // 기존 사용자 조회 (provider + providerId)
        User user = userRepository.findByProviderAndProviderId(request.getProvider(), providerId)
                .orElse(null);

        // 기존 사용자 없음 → 자동 회원가입
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .nickname(name)
                    .provider(request.getProvider())
                    .providerId(providerId)
                    .passwordHash(null)
                    .build();

            user = userRepository.save(user);
            log.info("Mock 소셜 로그인으로 새로운 사용자 등록: {} ({})", email, request.getProvider());
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // 프로필 정보 생성
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
