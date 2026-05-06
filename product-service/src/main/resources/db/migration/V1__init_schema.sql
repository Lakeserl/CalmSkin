CREATE TABLE categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    image_url VARCHAR(500),
    parent_id BIGINT REFERENCES categories(category_id),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_category_slug ON categories(slug);
CREATE INDEX idx_category_parent_id ON categories(parent_id);

CREATE TABLE brands (
    brand_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(500),
    origin_country VARCHAR(100),
    website_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_brand_slug ON brands(slug);

CREATE TABLE ingredients (
    ingredient_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    inci_name VARCHAR(300),
    description TEXT,
    benefits JSONB,
    side_effects TEXT,
    safety_level VARCHAR(20) DEFAULT 'SAFE',
    suitable_skin_types JSONB,
    avoid_skin_concerns JSONB,
    is_common_allergen BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ingredient_name ON ingredients(name);

CREATE TABLE tags (
    tag_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(60) NOT NULL UNIQUE,
    color_code VARCHAR(7)
);

CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(250) NOT NULL UNIQUE,
    sku VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    short_description VARCHAR(500),
    how_to_use TEXT,
    category_id BIGINT REFERENCES categories(category_id),
    brand_id BIGINT REFERENCES brands(brand_id),
    base_price NUMERIC(12, 0) NOT NULL,
    sale_price NUMERIC(12, 0),
    usage_step VARCHAR(20),
    suitable_skin_types JSONB,
    skin_concerns JSONB,
    volume_ml INTEGER,
    weight_g INTEGER,
    shelf_life_months INTEGER,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_new_arrival BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    view_count BIGINT NOT NULL DEFAULT 0,
    sold_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_slug ON products(slug);
CREATE INDEX idx_product_sku ON products(sku);
CREATE INDEX idx_product_category ON products(category_id);
CREATE INDEX idx_product_brand ON products(brand_id);
CREATE INDEX idx_product_status ON products(status);

CREATE TABLE product_variants (
    variant_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    name VARCHAR(100) NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,
    price NUMERIC(12, 0) NOT NULL,
    sale_price NUMERIC(12, 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_variant_product ON product_variants(product_id);
CREATE INDEX idx_variant_sku ON product_variants(sku);

CREATE TABLE product_images (
    image_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(200),
    display_order INTEGER DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_image_product ON product_images(product_id);

CREATE TABLE product_ingredients (
    product_ingredient_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(ingredient_id),
    concentration_percent NUMERIC(5, 2),
    display_order INTEGER DEFAULT 0,
    is_key_ingredient BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_product_ingredient UNIQUE (product_id, ingredient_id)
);

CREATE INDEX idx_pi_product ON product_ingredients(product_id);
CREATE INDEX idx_pi_ingredient ON product_ingredients(ingredient_id);

CREATE TABLE product_tags (
    product_tag_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    tag_id BIGINT NOT NULL REFERENCES tags(tag_id),
    CONSTRAINT uk_product_tag UNIQUE (product_id, tag_id)
);

CREATE TABLE review_summary (
    review_summary_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id) UNIQUE,
    average_rating NUMERIC(3, 2) DEFAULT 0.0,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    five_star_count INTEGER NOT NULL DEFAULT 0,
    four_star_count INTEGER NOT NULL DEFAULT 0,
    three_star_count INTEGER NOT NULL DEFAULT 0,
    two_star_count INTEGER NOT NULL DEFAULT 0,
    one_star_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_review_product ON review_summary(product_id);

CREATE TABLE ingredient_conflicts (
    conflict_id BIGSERIAL PRIMARY KEY,
    ingredient_a_id BIGINT NOT NULL REFERENCES ingredients(ingredient_id),
    ingredient_b_id BIGINT NOT NULL REFERENCES ingredients(ingredient_id),
    reason TEXT,
    severity VARCHAR(20),
    CONSTRAINT uk_ingredient_conflict UNIQUE (ingredient_a_id, ingredient_b_id)
);
