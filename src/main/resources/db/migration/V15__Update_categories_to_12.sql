-- Migration: Update categories to 12 categories
-- Frontend와 동기화된 카테고리 체계

-- 기존 카테고리 삭제 (참조 무결성 고려 필요 - 실제 운영 시 주의!)
DELETE FROM categories;

-- 새로운 12개 카테고리 + 미분류 삽입
INSERT INTO categories (category_code, category_name, icon_name, color_hex, sort_order) VALUES
    ('FOOD', '식비', 'restaurant', '#EF6C00', 1),
    ('CAFE_SNACK', '카페/간식', 'coffee', '#8D6E63', 2),
    ('TRANSPORT', '교통', 'directions_car', '#1E88E5', 3),
    ('HOUSING', '주거', 'home', '#546E7A', 4),
    ('COMMUNICATION', '통신/인터넷', 'signal_cellular_alt', '#3949AB', 5),
    ('SUBSCRIPTION', '구독', 'refresh', '#7E57C2', 6),
    ('LIVING', '생활', 'shopping_cart', '#43A047', 7),
    ('SHOPPING', '쇼핑', 'shopping_bag', '#EC407A', 8),
    ('HEALTH', '건강', 'local_pharmacy', '#E53935', 9),
    ('EDUCATION', '교육', 'school', '#5C6BC0', 10),
    ('CULTURE', '문화', 'movie', '#8E24AA', 11),
    ('INSURANCE', '보험', 'shield', '#00897B', 12),
    ('UNCATEGORIZED', '미분류', 'help_outline', '#9E9E9E', 99);

-- 기존 ETC, MEDICAL, EVENT는 삭제됨
-- ETC → UNCATEGORIZED로 마이그레이션 필요 시 별도 처리
