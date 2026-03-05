-- 카카오 알림 연동을 위한 토큰 컬럼 추가
ALTER TABLE talmo_users ADD COLUMN kakao_access_token VARCHAR(500);
ALTER TABLE talmo_users ADD COLUMN kakao_refresh_token VARCHAR(500);
