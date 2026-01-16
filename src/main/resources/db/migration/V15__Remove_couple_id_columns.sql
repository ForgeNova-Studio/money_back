-- =====================================================
-- V15: couple_id 컬럼 완전 제거
-- =====================================================
-- 설명: AccountBook 기반 아키텍처로 완전 전환
--       개발 기간이므로 하위 호환성 불필요
-- 실행: Supabase SQL Editor에서 수동 실행
-- =====================================================

-- 1. expenses 테이블
ALTER TABLE expenses DROP COLUMN IF EXISTS couple_id;
DROP INDEX IF EXISTS idx_expenses_couple_date;

-- 2. incomes 테이블
ALTER TABLE incomes DROP COLUMN IF EXISTS couple_id;
DROP INDEX IF EXISTS idx_incomes_couple_date;

-- 3. recurring_expenses 테이블
ALTER TABLE recurring_expenses DROP COLUMN IF EXISTS couple_id;
DROP INDEX IF EXISTS idx_recurring_expenses_couple_id;

-- =====================================================
-- 검증 쿼리 (실행 후 확인용)
-- =====================================================
-- SELECT column_name FROM information_schema.columns
-- WHERE table_name = 'expenses' AND column_name = 'couple_id';
-- (결과가 0개여야 성공)
