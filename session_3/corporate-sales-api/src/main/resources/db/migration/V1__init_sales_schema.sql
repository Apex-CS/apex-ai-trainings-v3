CREATE TABLE product (
    id              SERIAL PRIMARY KEY,
    product_code    VARCHAR(40) NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    list_price      NUMERIC(10, 2) NOT NULL
);

CREATE TABLE sale (
    id              SERIAL PRIMARY KEY,
    product_id      INTEGER NOT NULL REFERENCES product(id),
    purchase_date   TIMESTAMP NOT NULL,
    sale_price      NUMERIC(10, 2) NOT NULL,
    customer_name   VARCHAR(120) NOT NULL,
    customer_phone  VARCHAR(30) NOT NULL
);

CREATE INDEX idx_sale_product_id ON sale(product_id);
CREATE INDEX idx_sale_purchase_date ON sale(purchase_date DESC);
CREATE INDEX idx_product_code ON product(product_code);
