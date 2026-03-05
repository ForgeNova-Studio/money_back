-- Talmo 타이핑 연습 기록 테이블
-- MoneyFlow DB와 같은 PostgreSQL에 생성하되, talmo_ prefix로 구분

CREATE TABLE talmo_users (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE talmo_records (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES talmo_users(id),
  topic VARCHAR(100) NOT NULL,
  time_display VARCHAR(20),
  time_ms INTEGER NOT NULL,
  errors INTEGER DEFAULT 0,
  task_count INTEGER,
  completed_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_talmo_records_user ON talmo_records(user_id);
CREATE INDEX idx_talmo_records_date ON talmo_records(completed_at DESC);

-- 코테 문제 공유 테이블
CREATE TABLE talmo_problems (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES talmo_users(id),
  title VARCHAR(200) NOT NULL,
  source VARCHAR(50),            -- programmers, baekjoon, leetcode
  difficulty VARCHAR(20),        -- Lv.1~5
  problem_url VARCHAR(500),
  description TEXT,
  io_example TEXT,               -- 탭 구분 원본 텍스트
  io_explanation TEXT,            -- 입출력 예 설명
  solution_code TEXT,
  solution_note TEXT,
  tags VARCHAR(200),             -- 쉼표 구분: "문자열,DP,그리디"
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_talmo_problems_user ON talmo_problems(user_id);
CREATE INDEX idx_talmo_problems_date ON talmo_problems(created_at DESC);
