-- vV11: couples 테이블에 커플 전용 상세 정보 추가
-- 기념일, 애칭 등 관계 정보를 저장

ALTER TABLE couples 
ADD COLUMN IF NOT EXISTS anniversary DATE,
ADD COLUMN IF NOT EXISTS nickname1 VARCHAR(50),
ADD COLUMN IF NOT EXISTS nickname2 VARCHAR(50);

-- 컬럼 설명 추가
COMMENT ON COLUMN couples.anniversary IS '사귄 날짜 / 기념일';
COMMENT ON COLUMN couples.nickname1 IS 'user1의 애칭';
COMMENT ON COLUMN couples.nickname2 IS 'user2의 애칭';
