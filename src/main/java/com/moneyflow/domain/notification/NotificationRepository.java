package com.moneyflow.domain.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * 사용자별 알림 목록 조회 (페이징)
     */
    Page<Notification> findAllByUserUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 개수 조회
     */
    long countByUserUserIdAndIsReadFalse(UUID userId);

    /**
     * 사용자별 알림 목록 조회 (날짜 범위 + 페이징)
     */
    Page<Notification> findAllByUserUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID userId, LocalDateTime after, Pageable pageable);
}
