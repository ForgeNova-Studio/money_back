package com.moneyflow.service;

import com.moneyflow.domain.accountbook.AccountBookService;
import com.moneyflow.domain.token.RefreshToken;
import com.moneyflow.domain.token.RefreshTokenRepository;
import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserAuth;
import com.moneyflow.domain.user.UserAuthRepository;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.domain.verification.EmailVerification;
import com.moneyflow.domain.verification.EmailVerificationRepository;
import com.moneyflow.dto.request.ChangePasswordRequest;
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
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final GoogleOAuthService googleOAuthService;
    private final NaverOAuthService naverOAuthService;
    private final KakaoOAuthService kakaoOAuthService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountBookService accountBookService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("이미 사용 중인 이메일입니다");
        }

        // 이메일 인증 완료 여부 확인
        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndVerificationTypeAndVerifiedTrueOrderByVerifiedAtDesc(
                        request.getEmail(),
                        EmailVerification.VerificationType.SIGNUP)
                .orElseThrow(() -> new BusinessException("이메일 인증을 먼저 완료해주세요"));

        // 인증 완료 후 5분 이내 확인
        if (verification.isExpiredForRegistration()) {
            throw new BusinessException("인증 시간이 만료되었습니다. 다시 인증해주세요");
        }

        // 사용자 생성 (프로필 정보만)
        User user = User.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .gender(request.getGender())
                .build();

        user = userRepository.save(user);

        // EMAIL 인증 정보 생성 (UserAuth 테이블)
        UserAuth emailAuth = UserAuth.builder()
                .user(user)
                .provider(AuthProvider.EMAIL)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        userAuthRepository.save(emailAuth);

        accountBookService.createDefaultAccountBookIfMissing(user);

        // 사용한 인증 정보 삭제 (재사용 방지)
        emailVerificationRepository.delete(verification);

        log.info("새로운 사용자 등록: {}", user.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Refresh Token DB에 저장
        saveRefreshToken(refreshToken, user);

        return RegisterResponse.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            // 인증 시도
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            // 사용자 조회
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다"));

            log.info("사용자 로그인: {}", user.getEmail());

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // Refresh Token DB에 저장
            saveRefreshToken(refreshToken, user);

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
                // 카카오 API에서 이메일을 못 받았지만 SDK에서 받은 이메일이 있으면 사용
                if ((email == null || email.isBlank()) && request.getEmail() != null && !request.getEmail().isBlank()) {
                    email = request.getEmail();
                    log.info("카카오 API에서 이메일 없음 - SDK 이메일 사용: {}", email);
                }
            } else {
                throw new BusinessException("지원하지 않는 로그인 제공자입니다");
            }

            // 기존 사용자 조회 (UserAuth 테이블에서 provider + providerId로)
            UserAuth existingAuth = userAuthRepository.findByProviderAndProviderId(request.getProvider(), providerId)
                    .orElse(null);

            User user;
            // 기존 사용자 없음 → 자동 회원가입
            if (existingAuth == null) {
                // 이메일로 다른 provider의 기존 사용자가 있는지 확인
                if (email != null && !email.isBlank() && userRepository.existsByEmail(email)) {
                    // 기존 사용자의 로그인 방법 조회
                    User existingUser = userRepository.findByEmail(email).orElse(null);
                    if (existingUser != null) {
                        List<UserAuth> existingAuths = userAuthRepository.findByUser(existingUser);
                        String providers = existingAuths.stream()
                                .map(auth -> auth.getProvider().name())
                                .collect(java.util.stream.Collectors.joining(", "));
                        throw new BusinessException(
                                "이 이메일은 " + providers + " 로그인으로 가입되어 있습니다. 해당 방법으로 로그인해주세요.",
                                ErrorCode.EMAIL_REGISTERED_WITH_OTHER_PROVIDER);
                    }
                    throw new BusinessException("이미 다른 방법으로 가입된 이메일입니다", ErrorCode.EMAIL_ALREADY_EXISTS);
                }

                // 닉네임이 없으면 이메일 앞부분 사용
                if (name == null || name.isBlank()) {
                    name = email.split("@")[0];
                }

                // 사용자 생성 (프로필 정보만)
                user = User.builder()
                        .email(email)
                        .nickname(name)
                        .build();
                user = userRepository.save(user);

                // 소셜 로그인 인증 정보 생성 (UserAuth 테이블)
                UserAuth socialAuth = UserAuth.builder()
                        .user(user)
                        .provider(request.getProvider())
                        .providerId(providerId)
                        .build();
                userAuthRepository.save(socialAuth);

                accountBookService.createDefaultAccountBookIfMissing(user);
                log.info("소셜 로그인으로 새로운 사용자 등록: {} ({})", email, request.getProvider());
            } else {
                user = existingAuth.getUser();
                // 기존 사용자의 이메일이 비어있고, 새로 받은 이메일이 있으면 업데이트
                if ((user.getEmail() == null || user.getEmail().isBlank()) && email != null && !email.isBlank()) {
                    // 이메일 중복 체크 후 업데이트
                    if (!userRepository.existsByEmail(email)) {
                        user.setEmail(email);
                        userRepository.save(user);
                        log.info("기존 사용자 이메일 업데이트: {}", email);
                    } else {
                        // 이메일이 다른 계정에서 사용 중 - 기존 로그인 방법 안내
                        User otherUser = userRepository.findByEmail(email).orElse(null);
                        if (otherUser != null) {
                            List<UserAuth> otherAuths = userAuthRepository.findByUser(otherUser);
                            String providers = otherAuths.stream()
                                    .map(auth -> auth.getProvider().name())
                                    .collect(java.util.stream.Collectors.joining(", "));
                            throw new BusinessException(
                                    "이 이메일은 " + providers + " 로그인으로 가입되어 있습니다. 해당 방법으로 로그인해주세요.",
                                    ErrorCode.EMAIL_REGISTERED_WITH_OTHER_PROVIDER);
                        }
                        log.warn("이메일 업데이트 실패 - 이미 사용 중인 이메일: {}", email);
                    }
                }
                log.info("소셜 로그인: {} ({})", user.getEmail(), request.getProvider());
            }

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            // Refresh Token DB에 저장
            saveRefreshToken(refreshToken, user);

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

        // 기존 사용자 조회 (UserAuth 테이블에서 provider + providerId로)
        UserAuth existingAuth = userAuthRepository.findByProviderAndProviderId(request.getProvider(), providerId)
                .orElse(null);

        User user;
        // 기존 사용자 없음 → 자동 회원가입
        if (existingAuth == null) {
            // 사용자 생성 (프로필 정보만)
            user = User.builder()
                    .email(email)
                    .nickname(name)
                    .build();
            user = userRepository.save(user);

            // 소셜 로그인 인증 정보 생성
            UserAuth socialAuth = UserAuth.builder()
                    .user(user)
                    .provider(request.getProvider())
                    .providerId(providerId)
                    .build();
            userAuthRepository.save(socialAuth);

            accountBookService.createDefaultAccountBookIfMissing(user);
            log.info("Mock 소셜 로그인으로 새로운 사용자 등록: {} ({})", email, request.getProvider());
        } else {
            user = existingAuth.getUser();
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Refresh Token DB에 저장
        saveRefreshToken(refreshToken, user);

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

        // EMAIL provider 인증 정보가 없으면 소셜 로그인 사용자
        boolean hasEmailAuth = userAuthRepository.findByUserUserIdAndProvider(user.getUserId(), AuthProvider.EMAIL)
                .isPresent();
        if (!hasEmailAuth) {
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
     * 비밀번호 재설정 인증 코드 검증
     *
     * @param request 인증 코드 검증 요청
     * @return 검증 결과
     */
    @Transactional
    public VerificationResponse verifyPasswordResetCode(VerifyCodeRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

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

        // 인증 완료 처리
        verification.markAsVerified();
        emailVerificationRepository.save(verification);

        log.info("비밀번호 재설정 인증 코드 검증 완료: {}", email);

        return VerificationResponse.success("인증이 완료되었습니다. 새 비밀번호를 설정해주세요.");
    }

    /**
     * 비밀번호 재설정 (인증 완료 후)
     *
     * @param request 비밀번호 변경 요청
     * @return 재설정 결과
     */
    @Transactional
    public VerificationResponse resetPassword(ChangePasswordRequest request) {
        String email = request.getEmail();
        String newPassword = request.getNewPassword();

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("가입되지 않은 이메일입니다"));

        // EMAIL provider 인증 정보 조회 - 없으면 소셜 로그인 사용자
        UserAuth emailAuth = userAuthRepository.findByUserUserIdAndProvider(user.getUserId(), AuthProvider.EMAIL)
                .orElseThrow(() -> new BusinessException("소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다"));

        // 인증 완료된 정보 조회 (가장 최근 것)
        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndVerificationTypeAndVerifiedTrueOrderByVerifiedAtDesc(
                        email, EmailVerification.VerificationType.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException("인증을 먼저 완료해주세요"));

        // 인증 후 5분 이내인지 확인
        if (verification.isExpiredForRegistration()) {
            throw new BusinessException("인증 시간이 만료되었습니다. 다시 인증해주세요.");
        }

        // UserAuth 테이블에서 비밀번호 재설정
        emailAuth.setPasswordHash(passwordEncoder.encode(newPassword));
        userAuthRepository.save(emailAuth);

        // 사용된 인증 정보 삭제
        emailVerificationRepository.delete(verification);

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
            throw new UnauthorizedException("인증되지 않은 사용자입니다");
        }

        // UserDetails에서 userId 추출 (CustomUserDetailsService에서 username을
        // userId.toString()로 설정했음)
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new UnauthorizedException("인증 정보가 올바르지 않습니다");
        }

        String userIdString = ((UserDetails) principal).getUsername();
        UUID userId = UUID.fromString(userIdString);

        // DB에서 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다"));

        return UserInfoResponse.from(user);
    }

    /**
     * JWT Refresh Token으로 새로운 Access Token 발급 (Rotation 정책)
     *
     * Rotation 정책:
     * 1. 기존 Refresh Token 무효화
     * 2. 새 Refresh Token 발급 및 DB 저장
     * 3. 탈취된 토큰은 1회만 사용 가능
     */
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        // STEP 1: JWT 검증 (빠른 검증 - 서명, 만료시간)
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("유효하지 않거나 만료된 Refresh Token입니다");
        }

        // STEP 2: DB 검증 (상태 확인)
        String tokenHash = hashToken(refreshToken);
        RefreshToken refreshTokenEntity = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 Refresh Token입니다"));

        // DB 만료시간 확인
        if (refreshTokenEntity.isExpired()) {
            throw new UnauthorizedException("만료된 Refresh Token입니다");
        }

        // JWT에서 사용자 ID 추출
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        log.info("토큰 갱신 (Rotation): {} ({})", user.getEmail(), user.getUserId());

        // STEP 3: Rotation 정책 - 기존 토큰 무효화
        refreshTokenEntity.revoke();
        refreshTokenRepository.save(refreshTokenEntity);
        log.debug("기존 Refresh Token 무효화: {}", tokenHash.substring(0, 10) + "...");

        // STEP 4: 새로운 Access Token 및 Refresh Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        // STEP 5: 새 Refresh Token DB에 저장
        saveRefreshToken(newRefreshToken, user);

        // 프로필 정보 생성
        LoginResponse.UserProfile profile = LoginResponse.UserProfile.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImage(user.getProfileImageUrl())
                .build();

        return LoginResponse.builder()
                .userId(user.getUserId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .profile(profile)
                .build();
    }

    // ========== 개발용 메서드 ==========

    /**
     * [개발용] 이메일로 유저 조회
     * ️ 개발/테스트 전용 - 프로덕션 환경에서는 비활성화되어야 합니다.
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        log.info("[개발용] 유저 조회: {} ({})", user.getEmail(), user.getUserId());
        return UserInfoResponse.from(user);
    }

    /**
     * [개발용] 전체 유저 목록 조회
     * ️ 개발/테스트 전용 - 프로덕션 환경에서는 비활성화되어야 합니다.
     */
    @Transactional(readOnly = true)
    public java.util.List<UserInfoResponse> getAllUsers() {
        log.info("[개발용] 전체 유저 목록 조회");
        return userRepository.findAll().stream()
                .map(UserInfoResponse::from)
                .toList();
    }

    /**
     * [개발용] 유저 완전 삭제
     * ️ 개발/테스트 전용 - 프로덕션 환경에서는 비활성화되어야 합니다.
     * DB에서 유저를 완전히 삭제합니다 (되돌릴 수 없음).
     */
    @Transactional
    public void deleteUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        log.warn("[개발용] 유저 삭제: {} ({})", user.getEmail(), user.getUserId());
        userRepository.delete(user);
    }

    // ========== Refresh Token 관리 ==========

    /**
     * Refresh Token을 DB에 저장
     *
     * @param refreshToken JWT Refresh Token
     * @param user         사용자
     */
    private void saveRefreshToken(String refreshToken, User user) {
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

    /**
     * Refresh Token의 SHA-256 해시값 생성
     *
     * 보안을 위해 실제 토큰이 아닌 해시값만 DB에 저장합니다.
     *
     * @param token Refresh Token
     * @return SHA-256 해시값 (Base64 인코딩)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }

    /**
     * 로그아웃 (Refresh Token 무효화)
     *
     * @param refreshToken JWT Refresh Token
     */
    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);

        // DB에 토큰이 없어도 에러를 발생시키지 않음 (이미 로그아웃 상태로 간주)
        var tokenOptional = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOptional.isPresent()) {
            RefreshToken refreshTokenEntity = tokenOptional.get();
            refreshTokenEntity.revoke();
            refreshTokenRepository.save(refreshTokenEntity);

            log.info("로그아웃: 사용자 {} ({})",
                    refreshTokenEntity.getUser().getEmail(),
                    refreshTokenEntity.getUser().getUserId());
        } else {
            // 토큰이 없으면 로그 남기고 정상 처리 (DB 초기화 등의 경우)
            log.warn("로그아웃 요청: 유효하지 않거나 이미 삭제된 Refresh Token (해시: {})",
                    tokenHash.substring(0, Math.min(10, tokenHash.length())) + "...");
        }
    }
}
