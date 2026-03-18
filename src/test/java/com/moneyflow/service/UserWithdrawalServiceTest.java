package com.moneyflow.service;

import com.moneyflow.domain.couple.Couple;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CoupleRepository coupleRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserAgreementRepository userAgreementRepository;

    @InjectMocks
    private UserWithdrawalService userWithdrawalService;

    @Test
    @DisplayName("회원 탈퇴: 이메일 회원이면 비밀번호 확인 후 비cascade 데이터만 선삭제하고 사용자 삭제를 위임한다")
    void withdraw_emailUser_deletesLegacyDataAndReliesOnCascade() {
        UUID userId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();

        User withdrawingUser = user(userId, "withdraw@test.com", "탈퇴유저");
        User partnerUser = user(partnerId, "partner@test.com", "파트너");
        UserAuth emailAuth = UserAuth.builder()
                .user(withdrawingUser)
                .provider(AuthProvider.EMAIL)
                .passwordHash("encoded-password")
                .build();
        Couple couple = Couple.builder()
                .coupleId(UUID.randomUUID())
                .user1(withdrawingUser)
                .user2(partnerUser)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(withdrawingUser));
        when(userAuthRepository.findByUserUserIdAndProvider(userId, AuthProvider.EMAIL))
                .thenReturn(Optional.of(emailAuth));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);
        when(coupleRepository.findLinkedCoupleByUserId(userId)).thenReturn(Optional.of(couple));

        userWithdrawalService.withdraw(userId, WithdrawRequest.builder()
                .password("Password123!")
                .reason("서비스 미사용")
                .build());

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getUser()).isEqualTo(partnerUser);
        assertThat(notification.getTitle()).isEqualTo("커플 연동 해제");
        assertThat(notification.getMessage()).contains("탈퇴");

        var inOrder = inOrder(
                notificationRepository,
                userAgreementRepository,
                userAuthRepository,
                userRepository);

        inOrder.verify(notificationRepository).save(any(Notification.class));
        inOrder.verify(userAgreementRepository).deleteByUserUserId(userId);
        inOrder.verify(userAuthRepository).deleteByUserUserId(userId);
        inOrder.verify(userRepository).delete(withdrawingUser);
    }

    @Test
    @DisplayName("회원 탈퇴: 이메일 회원 비밀번호가 틀리면 삭제를 진행하지 않는다")
    void withdraw_throwsWhenPasswordIsInvalid() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "withdraw@test.com", "탈퇴유저");
        UserAuth emailAuth = UserAuth.builder()
                .user(user)
                .provider(AuthProvider.EMAIL)
                .passwordHash("encoded-password")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAuthRepository.findByUserUserIdAndProvider(userId, AuthProvider.EMAIL))
                .thenReturn(Optional.of(emailAuth));
        when(passwordEncoder.matches("WrongPassword!", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userWithdrawalService.withdraw(userId, WithdrawRequest.builder()
                .password("WrongPassword!")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("비밀번호");

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("회원 탈퇴: 소셜 로그인 사용자는 비밀번호 없이 탈퇴 가능")
    void withdraw_socialUser_skipsPasswordValidation() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "social@test.com", "소셜유저");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAuthRepository.findByUserUserIdAndProvider(userId, AuthProvider.EMAIL))
                .thenReturn(Optional.empty());
        when(coupleRepository.findLinkedCoupleByUserId(userId)).thenReturn(Optional.empty());

        userWithdrawalService.withdraw(userId, WithdrawRequest.builder().build());

        verifyNoInteractions(passwordEncoder);
        verify(userRepository).delete(user);
        verify(userAuthRepository).deleteByUserUserId(userId);
    }

    private User user(UUID userId, String email, String nickname) {
        return User.builder()
                .userId(userId)
                .email(email)
                .nickname(nickname)
                .build();
    }
}
