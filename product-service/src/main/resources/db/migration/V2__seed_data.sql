-- Categories
INSERT INTO categories (category_id, name, slug, description, display_order) VALUES
(1, 'Skincare', 'skincare', 'All skincare products', 1),
(2, 'Cleansers', 'cleansers', 'Facial cleansers', 1),
(3, 'Toners', 'toners', 'Facial toners', 2),
(4, 'Serums', 'serums', 'Treatment serums', 3),
(5, 'Moisturizers', 'moisturizers', 'Face moisturizers and creams', 4);

UPDATE categories SET parent_id = 1 WHERE category_id IN (2, 3, 4, 5);

-- Brands
INSERT INTO brands (brand_id, name, slug, origin_country) VALUES
(1, 'CalmSkin', 'calmskin', 'Vietnam'),
(2, 'COSRX', 'cosrx', 'South Korea'),
(3, 'Paula''s Choice', 'paulas-choice', 'USA');

-- Ingredients
INSERT INTO ingredients (ingredient_id, name, inci_name, safety_level, benefits, suitable_skin_types) VALUES
(1, 'Niacinamide', 'Niacinamide', 'SAFE', '["Brightening", "Pore Care"]', '["ALL"]'),
(2, 'Salicylic Acid', 'Salicylic Acid', 'CAUTION', '["Exfoliation", "Acne Care"]', '["OILY", "COMBINATION"]'),
(3, 'Hyaluronic Acid', 'Sodium Hyaluronate', 'SAFE', '["Hydration"]', '["ALL"]'),
(4, 'Vitamin C', 'Ascorbic Acid', 'CAUTION', '["Brightening", "Anti-aging"]', '["ALL"]');

-- Conflicts
INSERT INTO ingredient_conflicts (ingredient_a_id, ingredient_b_id, severity, reason) VALUES
(2, 4, 'HIGH', 'Using Salicylic Acid (BHA) and Vitamin C together can cause severe irritation and compromise the skin barrier.');

-- Tags
INSERT INTO tags (tag_id, name, slug, color_code) VALUES
(1, 'Vegan', 'vegan', '#4CAF50'),
(2, 'Cruelty-Free', 'cruelty-free', '#9C27B0'),
(3, 'Fragrance-Free', 'fragrance-free', '#2196F3'),
(4, 'Bestseller', 'bestseller', '#FFC107');

-- Reset sequences (PostgreSQL specific)
SELECT setval('categories_category_id_seq', (SELECT MAX(category_id) FROM categories));
SELECT setval('brands_brand_id_seq', (SELECT MAX(brand_id) FROM brands));
SELECT setval('ingredients_ingredient_id_seq', (SELECT MAX(ingredient_id) FROM ingredients));
SELECT setval('tags_tag_id_seq', (SELECT MAX(tag_id) FROM tags));
