package com.moneyflow.domain.notification;

import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.NotificationRequest;
import com.moneyflow.dto.response.NotificationResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final OneSignalService oneSignalService;

    /**
     * 알림 생성 (관리자용 - 특정 사용자)
     */
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        User targetUser = userRepository.findByEmail(request.getTargetEmail())
                .orElseThrow(() -> {
                    log.warn("Target user not found for notification: {}", request.getTargetEmail());
                    return new BusinessException("해당 이메일의 사용자를 찾을 수 없습니다: " + request.getTargetEmail(),
                            ErrorCode.INVALID_INPUT);
                });

        Notification notification = Notification.builder()
                .user(targetUser)
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification for user {}: {}", request.getTargetEmail(), saved.getNotificationId());

        // OneSignal 푸시 발송
        try {
            // targetUsreId (UUID)를 String으로 변환하여 전달
            oneSignalService.sendNotification(
                    request.getTitle(),
                    request.getMessage(),
                    java.util.List.of(targetUser.getUserId().toString()));
        } catch (Exception e) {
            log.error("Failed to trigger push notification", e);
        }

        return toResponse(saved);
    }

    /**
     * 내 알림 목록 조회 (페이징 + 일수 필터)
     *
     * @param userId   사용자 ID
     * @param days     최근 N일 이내 알림만 조회 (null이면 전체 조회)
     * @param pageable 페이징 정보
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Integer days, Pageable pageable) {
        if (days != null && days > 0) {
            LocalDateTime after = LocalDateTime.now().minusDays(days);
            return notificationRepository
                    .findAllByUserUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, after, pageable)
                    .map(this::toResponse);
        }
        return notificationRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다"));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("해당 알림에 접근할 권한이 없습니다");
        }

        if (!notification.getIsRead()) {
            notification.markAsRead();
            log.info("Marked notification {} as read", notificationId);
        }
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }

    /**
     * 전체 사용자에게 알림 발송 (관리자용)
     * 
     * @return 발송된 알림 수
     */
    @Transactional
    public int sendNotificationToAll(UUID adminId, NotificationRequest request) {
        // 관리자 권한 체크 (이메일 기반)
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UnauthorizedException("인증 정보가 올바르지 않습니다"));

        if (!isAdmin(admin.getEmail())) {
            throw new UnauthorizedException("관리자 권한이 필요합니다");
        }

        // 모든 사용자 조회
        java.util.List<User> allUsers = userRepository.findAll();
        int sentCount = 0;

        for (User user : allUsers) {
            Notification notification = Notification.builder()
                    .user(user)
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .type(request.getType() != null ? request.getType() : "NOTICE")
                    .build();
            notificationRepository.save(notification);
            sentCount++;
        }

        log.info("Admin {} sent notification to all users: {} notifications", adminId, sentCount);

        // OneSignal 전체 발송 (세그먼트 이용)
        try {
            oneSignalService.sendNotificationToAll(request.getTitle(), request.getMessage());
        } catch (Exception e) {
            log.error("Failed to trigger push notification to all", e);
        }

        return sentCount;
    }

    /**
     * 관리자 이메일 체크
     */
    private boolean isAdmin(String email) {
        // TODO: 관리자 목록을 DB나 설정 파일에서 관리하도록 변경 가능
        java.util.List<String> adminEmails = java.util.List.of(
                "hanwoolc95@gmail.com",
                "th82602662@gmail.com",
                "th8260@naver.com");
        return email != null && adminEmails.contains(email.toLowerCase());
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
