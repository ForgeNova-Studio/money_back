package com.moneyflow.service;

import com.moneyflow.domain.couple.CoupleRepository;
import com.moneyflow.domain.notification.Notification;
import com.moneyflow.domain.notification.NotificationRepository;
import com.moneyflow.domain.terms.UserAgreementRepository;
import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserAuth;
import com.moneyflow.domain.user.UserAuthRepository;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.WithdrawRequest;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final CoupleRepository coupleRepository;
    private final NotificationRepository notificationRepository;
    private final UserAgreementRepository userAgreementRepository;

    @Transactional
    public void withdraw(UUID userId, WithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        if (isEmailUser(userId)) {
            validatePasswordForWithdrawal(userId, request.getPassword());
        }

        notifyCouplePartnerOnWithdrawal(user);

        if (request.getReason() != null && !request.getReason().isBlank()) {
            log.info("회원 탈퇴 사유: userId={}, reason={}", userId, request.getReason());
        }

        deleteNonCascadeData(userId);
        userRepository.delete(user);

        log.info("회원 탈퇴 완료: userId={}, email={}", userId, user.getEmail());
    }

    private void deleteNonCascadeData(UUID userId) {
        userAgreementRepository.deleteByUserUserId(userId);
        log.debug("약관 동의 이력 선삭제 완료: userId={}", userId);

        userAuthRepository.deleteByUserUserId(userId);
        log.debug("인증 정보 선삭제 완료: userId={}", userId);
    }

    private boolean isEmailUser(UUID userId) {
        return userAuthRepository.findByUserUserIdAndProvider(userId, AuthProvider.EMAIL).isPresent();
    }

    private void validatePasswordForWithdrawal(UUID userId, String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("비밀번호를 입력해주세요", ErrorCode.INVALID_INPUT);
        }

        UserAuth emailAuth = userAuthRepository.findByUserUserIdAndProvider(userId, AuthProvider.EMAIL)
                .orElseThrow(() -> new BusinessException("이메일 인증 정보를 찾을 수 없습니다"));

        if (!passwordEncoder.matches(password, emailAuth.getPasswordHash())) {
            throw new BusinessException("비밀번호가 올바르지 않습니다", ErrorCode.INVALID_PASSWORD);
        }
    }

    private void notifyCouplePartnerOnWithdrawal(User withdrawingUser) {
        coupleRepository.findLinkedCoupleByUserId(withdrawingUser.getUserId())
                .ifPresent(couple -> {
                    User partner = couple.getUser1().getUserId().equals(withdrawingUser.getUserId())
                            ? couple.getUser2()
                            : couple.getUser1();

                    if (partner == null) {
                        return;
                    }

                    Notification notification = Notification.builder()
                            .user(partner)
                            .title("커플 연동 해제")
                            .message(withdrawingUser.getNickname() + "님이 회원을 탈퇴하여 커플 연동이 해제되었습니다.")
                            .type("COUPLE")
                            .build();
                    notificationRepository.save(notification);

                    log.info("커플 파트너에게 탈퇴 알림 전송: partnerId={}", partner.getUserId());
                });
    }
}
