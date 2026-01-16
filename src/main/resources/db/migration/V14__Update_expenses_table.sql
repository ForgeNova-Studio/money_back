-- V14: expenses 테이블 수정
-- account_book_id, funding_source 컬럼 추가

ALTER TABLE expenses 
ADD COLUMN IF NOT EXISTS account_book_id UUID REFERENCES account_books(account_book_id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS funding_source VARCHAR(20) DEFAULT 'PERSONAL';

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_expenses_account_book ON expenses(account_book_id, date DESC);

-- 컬럼 설명
COMMENT ON COLUMN expenses.account_book_id IS '소속 장부 ID';
COMMENT ON COLUMN expenses.funding_source IS '지출 출처 (PERSONAL: 개인, SHARED_POOL: 공금)';

-- incomes 테이블도 동일하게 수정
ALTER TABLE incomes 
ADD COLUMN IF NOT EXISTS account_book_id UUID REFERENCES account_books(account_book_id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS funding_source VARCHAR(20) DEFAULT 'PERSONAL';

CREATE INDEX IF NOT EXISTS idx_incomes_account_book ON incomes(account_book_id, date DESC);

COMMENT ON COLUMN incomes.account_book_id IS '소속 장부 ID';
COMMENT ON COLUMN incomes.funding_source IS '수입 출처 (PERSONAL: 개인, SHARED_POOL: 공금)';
