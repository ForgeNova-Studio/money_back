-- Create incomes table
CREATE TABLE incomes (
    income_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    couple_id UUID REFERENCES couples(couple_id) ON DELETE CASCADE,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    date DATE NOT NULL,
    source VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_incomes_user_date ON incomes(user_id, date DESC);
CREATE INDEX idx_incomes_couple_date ON incomes(couple_id, date DESC);
CREATE INDEX idx_incomes_source ON incomes(source);
CREATE INDEX idx_incomes_date ON incomes(date DESC);

-- Add comments
COMMENT ON TABLE incomes IS '수입 내역';
COMMENT ON COLUMN incomes.income_id IS '수입 고유 ID';
COMMENT ON COLUMN incomes.user_id IS '수입을 입력한 사용자';
COMMENT ON COLUMN incomes.couple_id IS '커플 모드인 경우 커플 ID (NULL 가능)';
COMMENT ON COLUMN incomes.amount IS '수입 금액 (원)';
COMMENT ON COLUMN incomes.date IS '수입 날짜';
COMMENT ON COLUMN incomes.source IS '수입 출처 (급여, 부수입, 상여금, 투자수익, 기타)';
COMMENT ON COLUMN incomes.description IS '수입 설명 (선택)';
COMMENT ON COLUMN incomes.created_at IS '생성 일시';
COMMENT ON COLUMN incomes.updated_at IS '수정 일시';
