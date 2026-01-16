-- V12: account_books 테이블 생성
-- 가계부/장부 (커플 생활비, 여행 등)

CREATE TABLE account_books (
    account_book_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    book_type VARCHAR(20) NOT NULL,
    couple_id UUID REFERENCES couples(couple_id) ON DELETE SET NULL,
    member_count INTEGER DEFAULT 2,
    description TEXT,
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT true,
    created_by UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_account_books_couple ON account_books(couple_id);
CREATE INDEX idx_account_books_created_by ON account_books(created_by);
CREATE INDEX idx_account_books_active ON account_books(is_active);

-- 테이블 및 컬럼 설명
COMMENT ON TABLE account_books IS '가계부/장부 (커플 생활비, 여행 등)';
COMMENT ON COLUMN account_books.name IS '장부 이름 (예: 2025 생활비, 일본 여행)';
COMMENT ON COLUMN account_books.book_type IS '장부 유형 (COUPLE_LIVING, TRIP, PROJECT)';
COMMENT ON COLUMN account_books.couple_id IS '커플 장부인 경우 커플 ID';
COMMENT ON COLUMN account_books.member_count IS '정산용 참여 인원수';
COMMENT ON COLUMN account_books.start_date IS '시작일 (여행 시작일 등)';
COMMENT ON COLUMN account_books.end_date IS '종료일';
COMMENT ON COLUMN account_books.is_active IS '활성 상태';
