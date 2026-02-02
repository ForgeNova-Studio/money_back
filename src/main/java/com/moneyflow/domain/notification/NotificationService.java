package com.moneyflow.domain.notification;

import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.NotificationRequest;
import com.moneyflow.dto.response.NotificationResponse;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 알림 생성 (관리자용)
     */
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("수신자를 찾을 수 없습니다"));

        Notification notification = Notification.builder()
                .user(targetUser)
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification for user {}: {}", request.getTargetUserId(), saved.getNotificationId());

        return toResponse(saved);
    }

    /**
     * 내 알림 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
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
        return sentCount;
    }

    /**
     * 관리자 이메일 체크
     */
    private boolean isAdmin(String email) {
        // TODO: 관리자 목록을 DB나 설정 파일에서 관리하도록 변경 가능
        java.util.List<String> adminEmails = java.util.List.of(
                "hanwoolc95@gmail.com");
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
