-- 여행 가계부 N빵 정산 기능을 위한 스키마 변경
-- V16: expenses 테이블 수정 및 expense_participants 테이블 추가

-- 1. expenses 테이블에 결제자 및 원본 장부 ID 컬럼 추가
ALTER TABLE expenses ADD COLUMN paid_by_user_id UUID;
ALTER TABLE expenses ADD COLUMN original_account_book_id UUID;

-- paid_by_user_id 외래키 설정
ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_paid_by
    FOREIGN KEY (paid_by_user_id) REFERENCES users(user_id);

-- 2. 지출 참여자 테이블 생성 (공용 지출의 분담자 관리)
CREATE TABLE expense_participants (
    expense_id UUID NOT NULL,
    user_id UUID NOT NULL,
    share_ratio DECIMAL(5,4) DEFAULT 1.0000,  -- 분담 비율 (1.0 = 100%)
    share_amount DECIMAL(12,2),               -- 계산된 분담 금액
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (expense_id, user_id),
    CONSTRAINT fk_participant_expense FOREIGN KEY (expense_id) REFERENCES expenses(expense_id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 인덱스 추가
CREATE INDEX idx_expenses_paid_by ON expenses(paid_by_user_id);
CREATE INDEX idx_expenses_original_book ON expenses(original_account_book_id);
CREATE INDEX idx_expense_participants_user ON expense_participants(user_id);

-- 3. 기존 공용 지출(SHARED_POOL)에 대해 paid_by를 user_id로 설정 (마이그레이션)
UPDATE expenses
SET paid_by_user_id = user_id
WHERE funding_source = 'SHARED_POOL' AND paid_by_user_id IS NULL;
