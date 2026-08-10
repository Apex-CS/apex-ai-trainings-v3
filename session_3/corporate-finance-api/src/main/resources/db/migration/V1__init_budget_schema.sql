CREATE TABLE budget (
    id              SERIAL PRIMARY KEY,
    area            VARCHAR(20) NOT NULL,
    fiscal_quarter  INTEGER NOT NULL,
    fiscal_year     INTEGER NOT NULL,
    budget          NUMERIC(10, 2) NOT NULL,
    user_modified   VARCHAR(30),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_budget_area_period UNIQUE (area, fiscal_quarter, fiscal_year),
    CONSTRAINT chk_budget_fiscal_quarter CHECK (fiscal_quarter BETWEEN 1 AND 4)
);

CREATE INDEX idx_budget_area ON budget(area);
CREATE INDEX idx_budget_fiscal_year ON budget(fiscal_year);
