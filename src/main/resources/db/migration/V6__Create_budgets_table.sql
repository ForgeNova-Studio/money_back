-- 예산(목표 소비) 테이블 생성
CREATE TABLE budgets (
    budget_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL CHECK (month >= 1 AND month <= 12),
    target_amount DECIMAL(12, 2) NOT NULL CHECK (target_amount > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT budgets_user_year_month_unique UNIQUE (user_id, year, month)
);

-- 인덱스 생성
CREATE INDEX idx_budgets_user_id ON budgets(user_id);
CREATE INDEX idx_budgets_year_month ON budgets(year, month);

-- 코멘트 추가
COMMENT ON TABLE budgets IS '사용자별 월간 예산(목표 소비) 정보';
COMMENT ON COLUMN budgets.budget_id IS '예산 ID (UUID)';
COMMENT ON COLUMN budgets.user_id IS '사용자 ID';
COMMENT ON COLUMN budgets.year IS '년도';
COMMENT ON COLUMN budgets.month IS '월 (1-12)';
COMMENT ON COLUMN budgets.target_amount IS '목표 금액';
COMMENT ON COLUMN budgets.created_at IS '생성 일시';
COMMENT ON COLUMN budgets.updated_at IS '수정 일시';
