package com.moneyflow.service;

import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.domain.verification.EmailVerification;
import com.moneyflow.domain.verification.EmailVerificationRepository;
import com.moneyflow.dto.request.LoginRequest;
import com.moneyflow.dto.request.RegisterRequest;
import com.moneyflow.dto.request.ResetPasswordRequest;
import com.moneyflow.dto.request.SendCodeRequest;
import com.moneyflow.dto.request.SocialLoginRequest;
import com.moneyflow.dto.request.VerifyCodeRequest;
import com.moneyflow.dto.response.LoginResponse;
import com.moneyflow.dto.response.RegisterResponse;
import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.dto.response.VerificationResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

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
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;

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

    /**
     * 회원가입 인증 코드 발송
     */
    @Transactional
    public VerificationResponse sendSignupCode(SendCodeRequest request) {
        String email = request.getEmail();

        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("이미 가입된 이메일입니다");
        }

        // 기존 미완료 인증 코드 삭제
        emailVerificationRepository.deleteByEmailAndVerifiedFalse(email);

        // 인증 코드 생성
        String code = emailService.generateVerificationCode();

        // 인증 정보 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .verificationType(EmailVerification.VerificationType.SIGNUP)
                .build();
        emailVerificationRepository.save(verification);

        // 이메일 발송
        emailService.sendSignupVerificationEmail(email, code);

        log.info("회원가입 인증 코드 발송: {}", email);

        return VerificationResponse.success("인증 코드가 발송되었습니다. 이메일을 확인해주세요.");
    }

    /**
     * 회원가입 인증 코드 검증
     */
    @Transactional
    public VerificationResponse verifySignupCode(VerifyCodeRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        // 인증 정보 조회
        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndVerificationTypeAndVerifiedFalseOrderByCreatedAtDesc(
                        email, EmailVerification.VerificationType.SIGNUP)
                .orElseThrow(() -> new BusinessException("인증 코드를 찾을 수 없습니다"));

        // 만료 확인
        if (verification.isExpired()) {
            throw new BusinessException("인증 코드가 만료되었습니다. 다시 요청해주세요.");
        }

        // 코드 확인
        if (!verification.getVerificationCode().equals(code)) {
            throw new BusinessException("인증 코드가 일치하지 않습니다");
        }

        // 인증 완료 처리
        verification.markAsVerified();
        emailVerificationRepository.save(verification);

        log.info("회원가입 인증 코드 검증 완료: {}", email);

        return VerificationResponse.success("인증이 완료되었습니다");
    }

    /**
     * 비밀번호 재설정 인증 코드 발송
     */
    @Transactional
    public VerificationResponse sendPasswordResetCode(SendCodeRequest request) {
        String email = request.getEmail();

        // 가입된 이메일인지 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("가입되지 않은 이메일입니다"));

        // 소셜 로그인 사용자는 비밀번호 재설정 불가
        if (user.getProvider() != AuthProvider.EMAIL) {
            throw new BusinessException("소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다");
        }

        // 기존 미완료 인증 코드 삭제
        emailVerificationRepository.deleteByEmailAndVerifiedFalse(email);

        // 인증 코드 생성
        String code = emailService.generateVerificationCode();

        // 인증 정보 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .verificationType(EmailVerification.VerificationType.PASSWORD_RESET)
                .build();
        emailVerificationRepository.save(verification);

        // 이메일 발송
        emailService.sendPasswordResetEmail(email, code);

        log.info("비밀번호 재설정 인증 코드 발송: {}", email);

        return VerificationResponse.success("인증 코드가 발송되었습니다. 이메일을 확인해주세요.");
    }

    /**
     * 비밀번호 재설정
     */
    @Transactional
    public VerificationResponse resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String code = request.getCode();
        String newPassword = request.getNewPassword();

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("가입되지 않은 이메일입니다"));

        // 소셜 로그인 사용자는 비밀번호 재설정 불가
        if (user.getProvider() != AuthProvider.EMAIL) {
            throw new BusinessException("소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다");
        }

        // 인증 정보 조회
        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndVerificationTypeAndVerifiedFalseOrderByCreatedAtDesc(
                        email, EmailVerification.VerificationType.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException("인증 코드를 찾을 수 없습니다"));

        // 만료 확인
        if (verification.isExpired()) {
            throw new BusinessException("인증 코드가 만료되었습니다. 다시 요청해주세요.");
        }

        // 코드 확인
        if (!verification.getVerificationCode().equals(code)) {
            throw new BusinessException("인증 코드가 일치하지 않습니다");
        }

        // 비밀번호 재설정
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 인증 완료 처리
        verification.markAsVerified();
        emailVerificationRepository.save(verification);

        log.info("비밀번호 재설정 완료: {}", email);

        return VerificationResponse.success("비밀번호가 재설정되었습니다");
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser() {
        // SecurityContext에서 현재 인증된 사용자 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("인증되지 않은 사용자입니다");
        }

        // UserDetails에서 userId 추출 (CustomUserDetailsService에서 username을 userId.toString()로 설정했음)
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new BusinessException("인증 정보가 올바르지 않습니다");
        }

        String userIdString = ((UserDetails) principal).getUsername();
        UUID userId = UUID.fromString(userIdString);

        // DB에서 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        return UserInfoResponse.from(user);
    }
}
