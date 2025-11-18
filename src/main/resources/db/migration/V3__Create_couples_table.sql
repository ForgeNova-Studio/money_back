-- Create couples table
CREATE TABLE couples (
    couple_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user1_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    user2_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    invite_code VARCHAR(10) UNIQUE,
    code_expires_at TIMESTAMP,
    linked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_different_users CHECK (user1_id != user2_id)
);

-- Create indexes
CREATE INDEX idx_couples_invite_code ON couples(invite_code);
CREATE INDEX idx_couples_user1 ON couples(user1_id);
CREATE INDEX idx_couples_user2 ON couples(user2_id);

-- Add comments
COMMENT ON TABLE couples IS '커플 그룹 정보';
COMMENT ON COLUMN couples.user1_id IS '초대를 생성한 사용자';
COMMENT ON COLUMN couples.user2_id IS '초대를 수락한 사용자 (NULL 가능)';
COMMENT ON COLUMN couples.invite_code IS '초대 코드 (6자리 영숫자)';
COMMENT ON COLUMN couples.code_expires_at IS '초대 코드 만료 시간';
COMMENT ON COLUMN couples.linked_at IS '커플 연동 완료 시간';
