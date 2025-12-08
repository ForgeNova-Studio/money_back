-- Refresh Token 관리 테이블 생성
-- 로그아웃, 토큰 무효화, Rotation 정책을 위한 테이블

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 사용자별 토큰 조회 인덱스
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 토큰 해시 조회 인덱스 (refresh, logout 시 사용)
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- 무효화되지 않은 토큰 조회 인덱스
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);

-- 복합 인덱스: 토큰 해시 + 무효화 여부 (가장 빈번한 쿼리 최적화)
CREATE INDEX idx_refresh_tokens_hash_revoked ON refresh_tokens(token_hash, revoked);

COMMENT ON TABLE refresh_tokens IS 'Refresh Token 관리 테이블 - 로그아웃 및 토큰 무효화 지원';
COMMENT ON COLUMN refresh_tokens.id IS 'Refresh Token 고유 ID';
COMMENT ON COLUMN refresh_tokens.user_id IS '사용자 ID (외래키)';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'Refresh Token의 SHA-256 해시값 (보안)';
COMMENT ON COLUMN refresh_tokens.expires_at IS '토큰 만료 시간 (30일)';
COMMENT ON COLUMN refresh_tokens.created_at IS '토큰 생성 시간';
COMMENT ON COLUMN refresh_tokens.revoked IS '토큰 무효화 여부 (true: 로그아웃/무효화됨)';
