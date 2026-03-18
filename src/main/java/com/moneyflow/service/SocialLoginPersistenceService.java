package com.moneyflow.service;

import com.moneyflow.domain.accountbook.AccountBookService;
import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserAuth;
import com.moneyflow.domain.user.UserAuthRepository;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.response.LoginResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialLoginPersistenceService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final AccountBookService accountBookService;
    private final TokenService tokenService;

    @Transactional
    public LoginResponse loginOrRegister(AuthProvider provider, String email, String providerId, String name) {
        UserAuth existingAuth = userAuthRepository.findByProviderAndProviderId(provider, providerId)
                .orElse(null);

        User user;
        if (existingAuth == null) {
            ensureEmailIsAvailable(email);

            if (name == null) {
                name = deriveNameFromEmail(email);
            }

            user = User.builder()
                    .email(email)
                    .nickname(name)
                    .build();
            user = userRepository.save(user);

            UserAuth socialAuth = UserAuth.builder()
                    .user(user)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            userAuthRepository.save(socialAuth);

            accountBookService.createDefaultAccountBookIfMissing(user);
            log.info("소셜 로그인으로 새로운 사용자 등록: {} ({})", email, provider);
        } else {
            user = existingAuth.getUser();
            updateEmailIfMissing(user, email);
            log.info("소셜 로그인: {} ({})", user.getEmail(), provider);
        }

        return tokenService.issueLoginResponse(user);
    }

    @Transactional
    public LoginResponse mockLogin(AuthProvider provider, String email, String providerId, String name) {
        UserAuth existingAuth = userAuthRepository.findByProviderAndProviderId(provider, providerId)
                .orElse(null);

        User user;
        if (existingAuth == null) {
            user = User.builder()
                    .email(email)
                    .nickname(name)
                    .build();
            user = userRepository.save(user);

            UserAuth socialAuth = UserAuth.builder()
                    .user(user)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            userAuthRepository.save(socialAuth);

            accountBookService.createDefaultAccountBookIfMissing(user);
            log.info("Mock 소셜 로그인으로 새로운 사용자 등록: {} ({})", email, provider);
        } else {
            user = existingAuth.getUser();
        }

        return tokenService.issueLoginResponse(user);
    }

    private void ensureEmailIsAvailable(String email) {
        if (!userRepository.existsByEmail(email)) {
            return;
        }

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

    private void updateEmailIfMissing(User user, String email) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return;
        }

        if (!userRepository.existsByEmail(email)) {
            user.setEmail(email);
            userRepository.save(user);
            log.info("기존 사용자 이메일 업데이트: {}", email);
            return;
        }

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

    private String deriveNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }
        return "사용자";
    }
}
