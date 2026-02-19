-- V7: 소셜 로그인 지원을 위한 users 테이블 필드 추가
-- provider: 로그인 제공자 (EMAIL, GOOGLE, NAVER, KAKAO)
-- provider_id: 소셜 로그인 제공자의 사용자 고유 ID

-- provider 컬럼 추가 (기본값: EMAIL)
ALTER TABLE users
ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'EMAIL';

-- provider_id 컬럼 추가 (소셜 로그인 시 사용)
ALTER TABLE users
ADD COLUMN provider_id VARCHAR(255);

-- password_hash를 nullable로 변경 (소셜 로그인 시 비밀번호 불필요)
ALTER TABLE users
ALTER COLUMN password_hash DROP NOT NULL;

-- provider와 provider_id 조합에 유니크 제약 추가 (소셜 로그인 중복 방지)
CREATE UNIQUE INDEX idx_users_provider_provider_id ON users(provider, provider_id) WHERE provider_id IS NOT NULL;

-- 이메일 로그인의 경우 기존처럼 이메일만으로 유니크
-- 소셜 로그인의 경우 provider + provider_id로 유니크
COMMENT ON COLUMN users.provider IS '로그인 제공자: EMAIL, GOOGLE, NAVER, KAKAO';
COMMENT ON COLUMN users.provider_id IS '소셜 로그인 제공자의 사용자 고유 ID';
