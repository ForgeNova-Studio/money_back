-- V13: account_book_members 테이블 생성
-- 장부 참여자 매핑 (N:N)

CREATE TABLE account_book_members (
    account_book_id UUID NOT NULL REFERENCES account_books(account_book_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_book_id, user_id)
);

-- 인덱스 생성
CREATE INDEX idx_account_book_members_user ON account_book_members(user_id);

-- 테이블 및 컬럼 설명
COMMENT ON TABLE account_book_members IS '장부 참여자 매핑 테이블';
COMMENT ON COLUMN account_book_members.role IS '역할 (OWNER, MEMBER)';
COMMENT ON COLUMN account_book_members.joined_at IS '참여한 시간';
