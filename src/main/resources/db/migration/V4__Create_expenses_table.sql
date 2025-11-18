-- Create expenses table
CREATE TABLE expenses (
    expense_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    couple_id UUID REFERENCES couples(couple_id) ON DELETE CASCADE,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount >= 0),
    date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    merchant VARCHAR(255),
    memo TEXT,
    payment_method VARCHAR(20),
    image_url TEXT,
    is_auto_categorized BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_expenses_user_date ON expenses(user_id, date DESC);
CREATE INDEX idx_expenses_couple_date ON expenses(couple_id, date DESC);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_expenses_date ON expenses(date DESC);

-- Add comments
COMMENT ON TABLE expenses IS '지출 내역';
COMMENT ON COLUMN expenses.user_id IS '지출을 입력한 사용자';
COMMENT ON COLUMN expenses.couple_id IS '커플 모드인 경우 커플 ID (NULL 가능)';
COMMENT ON COLUMN expenses.amount IS '지출 금액 (원)';
COMMENT ON COLUMN expenses.date IS '지출 날짜';
COMMENT ON COLUMN expenses.category IS '카테고리 코드';
COMMENT ON COLUMN expenses.merchant IS '가맹점명';
COMMENT ON COLUMN expenses.memo IS '메모';
COMMENT ON COLUMN expenses.payment_method IS '결제 수단 (CARD, CASH, TRANSFER)';
COMMENT ON COLUMN expenses.image_url IS 'OCR 원본 이미지 URL';
COMMENT ON COLUMN expenses.is_auto_categorized IS 'AI 자동 분류 여부';
