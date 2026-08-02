CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    total DECIMAL(18,2) NOT NULL,
    version BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_carts_user_id UNIQUE (user_id)
) ENGINE=InnoDB;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(18,2) NOT NULL,
    version BIGINT,
    cart_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart_id FOREIGN KEY (cart_id) REFERENCES carts (id)
) ENGINE=InnoDB;
