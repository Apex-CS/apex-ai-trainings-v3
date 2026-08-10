CREATE TABLE app_restarts (
    id              SERIAL PRIMARY KEY,
    app_name        VARCHAR(60) NOT NULL,
    user_requested  VARCHAR(100) NOT NULL,
    operation_date  TIMESTAMP NOT NULL DEFAULT NOW(),
    operation_done  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_app_restarts_app_name ON app_restarts(app_name);
CREATE INDEX idx_app_restarts_operation_date ON app_restarts(operation_date DESC);
