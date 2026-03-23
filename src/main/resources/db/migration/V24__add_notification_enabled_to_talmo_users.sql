-- talmo_users 테이블에 카카오 알림 수신 여부 컬럼 추가
-- 기존 유저는 기본값 TRUE로 설정
ALTER TABLE talmo_users
    ADD COLUMN notification_enabled BOOLEAN NOT NULL DEFAULT TRUE;
