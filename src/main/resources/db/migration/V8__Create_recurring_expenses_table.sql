-- V8: 고정비 및 구독료 관리 테이블 생성
-- recurring_expenses: 월세, 구독료 등 반복적인 지출 관리

CREATE TABLE recurring_expenses (
    recurring_expense_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    couple_id UUID REFERENCES couples(couple_id) ON DELETE SET NULL,

    -- 기본 정보
    name VARCHAR(100) NOT NULL,                    -- 고정비 이름 (예: 넷플릭스, 월세)
    amount DECIMAL(15, 2) NOT NULL,                -- 금액
    category VARCHAR(50) NOT NULL,                 -- 카테고리
    description TEXT,                              -- 설명

    -- 반복 정보
    recurring_type VARCHAR(20) NOT NULL,           -- MONTHLY, YEARLY, WEEKLY
    start_date DATE NOT NULL,                      -- 시작일
    end_date DATE,                                 -- 종료일 (nullable, null이면 무기한)
    day_of_month INTEGER,                          -- 매달 몇일 (1~31, MONTHLY일 때)
    day_of_week INTEGER,                           -- 요일 (0=일요일, 6=토요일, WEEKLY일 때)
    next_payment_date DATE NOT NULL,               -- 다음 결제 예정일

    -- 구독료 관련
    is_subscription BOOLEAN DEFAULT FALSE,         -- 구독료 여부
    subscription_provider VARCHAR(100),            -- 구독 제공자 (넷플릭스, 스포티파이 등)

    -- 알림 및 변동 감지
    notification_enabled BOOLEAN DEFAULT TRUE,     -- 알림 활성화 여부
    last_amount DECIMAL(15, 2),                    -- 이전 금액 (변동 감지용)
    last_payment_date DATE,                        -- 마지막 결제일

    -- 자동 탐지 관련
    auto_detected BOOLEAN DEFAULT FALSE,           -- 자동 탐지된 항목 여부
    detection_confidence DECIMAL(3, 2),            -- 탐지 신뢰도 (0.00 ~ 1.00)

    -- 메타 정보
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_recurring_user_id ON recurring_expenses(user_id);
CREATE INDEX idx_recurring_couple_id ON recurring_expenses(couple_id);
CREATE INDEX idx_recurring_next_payment ON recurring_expenses(next_payment_date);
CREATE INDEX idx_recurring_subscription ON recurring_expenses(is_subscription);

-- 코멘트 추가
COMMENT ON TABLE recurring_expenses IS '고정비 및 구독료 관리 테이블';
COMMENT ON COLUMN recurring_expenses.recurring_type IS '반복 주기: MONTHLY(월별), YEARLY(연별), WEEKLY(주별)';
COMMENT ON COLUMN recurring_expenses.is_subscription IS '구독료 여부 (true: 구독료, false: 일반 고정비)';
COMMENT ON COLUMN recurring_expenses.auto_detected IS '자동 탐지된 항목 여부';
COMMENT ON COLUMN recurring_expenses.detection_confidence IS '자동 탐지 신뢰도 (0.0 ~ 1.0)';
