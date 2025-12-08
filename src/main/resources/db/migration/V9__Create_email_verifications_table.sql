-- 이메일 인증 테이블 생성
CREATE TABLE IF NOT EXISTS email_verifications (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    verification_code VARCHAR(6) NOT NULL,
    verification_type VARCHAR(20) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,

    CONSTRAINT chk_verification_type CHECK (verification_type IN ('SIGNUP', 'PASSWORD_RESET'))
);

-- 인덱스 생성
CREATE INDEX idx_email_verifications_email ON email_verifications(email);
CREATE INDEX idx_email_verifications_code ON email_verifications(verification_code);
CREATE INDEX idx_email_verifications_expires_at ON email_verifications(expires_at);

-- 코멘트 추가
COMMENT ON TABLE email_verifications IS '이메일 인증 정보';
COMMENT ON COLUMN email_verifications.id IS 'UUID 기본키';
COMMENT ON COLUMN email_verifications.email IS '이메일 주소';
COMMENT ON COLUMN email_verifications.verification_code IS '6자리 인증 코드';
COMMENT ON COLUMN email_verifications.verification_type IS '인증 유형 (SIGNUP: 회원가입, PASSWORD_RESET: 비밀번호 재설정)';
COMMENT ON COLUMN email_verifications.verified IS '인증 완료 여부';
COMMENT ON COLUMN email_verifications.expires_at IS '만료 시간';
COMMENT ON COLUMN email_verifications.created_at IS '생성 시간';
COMMENT ON COLUMN email_verifications.verified_at IS '인증 완료 시간';
