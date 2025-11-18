-- Create categories table
CREATE TABLE categories (
    category_code VARCHAR(50) PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL,
    icon_name VARCHAR(50),
    color_hex VARCHAR(7),
    sort_order INT
);

-- Insert default categories
INSERT INTO categories (category_code, category_name, icon_name, color_hex, sort_order) VALUES
    ('FOOD', '식비', 'restaurant', '#FF6B6B', 1),
    ('TRANSPORT', '교통', 'directions_car', '#4ECDC4', 2),
    ('SHOPPING', '쇼핑', 'shopping_bag', '#FFE66D', 3),
    ('CULTURE', '문화생활', 'movie', '#A8E6CF', 4),
    ('HOUSING', '주거/통신', 'home', '#95E1D3', 5),
    ('MEDICAL', '의료/건강', 'local_hospital', '#F38181', 6),
    ('EDUCATION', '교육', 'school', '#AA96DA', 7),
    ('EVENT', '경조사', 'card_giftcard', '#FCBAD3', 8),
    ('ETC', '기타', 'more_horiz', '#C7CEEA', 9);

-- Add comment
COMMENT ON TABLE categories IS '지출 카테고리 마스터';
