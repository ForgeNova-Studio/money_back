-- 회원가입 시 성별 필드 추가
-- 참고: Supabase 사용 시 SQL Editor에서 직접 실행 필요

-- users 테이블에 gender 컬럼 추가
ALTER TABLE users
ADD COLUMN gender VARCHAR(10);

-- 기존 사용자는 NULL 허용 (선택사항)
-- 신규 회원가입 시에는 NOT NULL로 입력됨

-- 인덱스 추가 (성별로 통계를 뽑을 경우 유용)
CREATE INDEX idx_users_gender ON users(gender);
