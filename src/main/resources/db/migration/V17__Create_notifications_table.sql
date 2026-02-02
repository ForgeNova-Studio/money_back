-- 알림(Notification) 테이블 생성
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = false;

-- 코멘트 추가
COMMENT ON TABLE notifications IS '사용자 알림 정보';
COMMENT ON COLUMN notifications.notification_id IS '알림 ID (UUID)';
COMMENT ON COLUMN notifications.user_id IS '수신자 사용자 ID';
COMMENT ON COLUMN notifications.title IS '알림 제목';
COMMENT ON COLUMN notifications.message IS '알림 내용';
COMMENT ON COLUMN notifications.type IS '알림 유형 (NOTICE, PERSONAL, ETC)';
COMMENT ON COLUMN notifications.is_read IS '읽음 여부';
COMMENT ON COLUMN notifications.created_at IS '생성 일시';
COMMENT ON COLUMN notifications.read_at IS '읽은 일시';
