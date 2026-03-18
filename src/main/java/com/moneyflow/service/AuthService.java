package com.moneyflow.service;

import com.moneyflow.domain.accountbook.AccountBookService;
import com.moneyflow.domain.terms.TermsService;
import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserAuth;
import com.moneyflow.domain.user.UserAuthRepository;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.LoginRequest;
import com.moneyflow.dto.request.RegisterRequest;
import com.moneyflow.dto.response.LoginResponse;
import com.moneyflow.dto.response.RegisterResponse;
import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AccountBookService accountBookService;
    private final TermsService termsService;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;
    private final DevelopmentOnlyAccessGuard developmentOnlyAccessGuard;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("이미 사용 중인 이메일입니다");
        }

        emailVerificationService.consumeVerifiedSignup(request.getEmail());

        User user = User.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .gender(request.getGender())
                .build();
        user = userRepository.save(user);

        UserAuth emailAuth = UserAuth.builder()
                .user(user)
                .provider(AuthProvider.EMAIL)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        userAuthRepository.save(emailAuth);

        accountBookService.createDefaultAccountBookIfMissing(user);

        if (request.getAgreements() != null && !request.getAgreements().isEmpty()) {
            termsService.saveAgreements(user.getUserId(), request.getAgreements(), null, null);
        }

        log.info("새로운 사용자 등록: {}", user.getEmail());
        return tokenService.issueRegisterResponse(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다"));

            log.info("사용자 로그인: {}", user.getEmail());
            return tokenService.issueLoginResponse(user);
        } catch (BadCredentialsException e) {
            log.warn("로그인 실패: {}", request.getEmail());
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw UnauthorizedException.authentication("인증되지 않은 사용자입니다");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw UnauthorizedException.authentication("인증 정보가 올바르지 않습니다");
        }

        String userIdString = ((UserDetails) principal).getUsername();
        UUID userId = UUID.fromString(userIdString);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다"));

        return UserInfoResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getUserByEmail(String email) {
        developmentOnlyAccessGuard.validate("개발용 유저 조회");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        log.info("[개발용] 유저 조회: {} ({})", user.getEmail(), user.getUserId());
        return UserInfoResponse.from(user);
    }

    @Transactional(readOnly = true)
    public java.util.List<UserInfoResponse> getAllUsers() {
        developmentOnlyAccessGuard.validate("개발용 전체 유저 조회");
        log.info("[개발용] 전체 유저 목록 조회");
        return userRepository.findAll().stream()
                .map(UserInfoResponse::from)
                .toList();
    }

    @Transactional
    public void deleteUserByEmail(String email) {
        developmentOnlyAccessGuard.validate("개발용 유저 삭제");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        log.warn("[개발용] 유저 삭제: {} ({})", user.getEmail(), user.getUserId());
        userRepository.delete(user);
    }
}
