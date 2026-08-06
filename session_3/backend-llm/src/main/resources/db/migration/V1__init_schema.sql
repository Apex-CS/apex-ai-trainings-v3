CREATE TABLE ingested_documents (
    id              BIGSERIAL PRIMARY KEY,
    document_id     VARCHAR(255) NOT NULL UNIQUE,
    title           VARCHAR(512),
    source_path     VARCHAR(1024),
    version         INTEGER NOT NULL DEFAULT 1,
    chunk_count     INTEGER NOT NULL DEFAULT 0,
    content_hash    VARCHAR(64),
    ingested_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ingested_documents_document_id ON ingested_documents(document_id);

-- Sample relational data for the SQL retrieval tool
CREATE TABLE owasp_top_risks (
    id          SERIAL PRIMARY KEY,
    year        INTEGER NOT NULL,
    rank        INTEGER NOT NULL,
    category    VARCHAR(255) NOT NULL,
    description TEXT NOT NULL
);

INSERT INTO owasp_top_risks (year, rank, category, description) VALUES
(2021, 1, 'Broken Access Control', 'Restrictions on authenticated users are not properly enforced.'),
(2021, 2, 'Cryptographic Failures', 'Failures related to cryptography which often lead to sensitive data exposure.'),
(2021, 3, 'Injection', 'User-supplied data is not validated, filtered, or sanitized by the application.'),
(2021, 4, 'Insecure Design', 'Missing or ineffective control design representing a broad category of weaknesses.'),
(2021, 5, 'Security Misconfiguration', 'Missing appropriate security hardening across any part of the application stack.'),
(2021, 6, 'Vulnerable and Outdated Components', 'Use of components with known vulnerabilities.'),
(2021, 7, 'Identification and Authentication Failures', 'Confirmation of the user identity, authentication, and session management.'),
(2021, 8, 'Software and Data Integrity Failures', 'Code and infrastructure that does not protect against integrity violations.'),
(2021, 9, 'Security Logging and Monitoring Failures', 'Insufficient logging, detection, monitoring, and active response.'),
(2021, 10, 'Server-Side Request Forgery', 'SSRF flaws occur when a web application fetches a remote resource without validating the user-supplied URL.');

CREATE TABLE security_controls (
    id          SERIAL PRIMARY KEY,
    control_id  VARCHAR(64) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    category    VARCHAR(128) NOT NULL,
    status      VARCHAR(32) NOT NULL,
    owner       VARCHAR(128)
);

INSERT INTO security_controls (control_id, name, category, status, owner) VALUES
('AC-01', 'Role-Based Access Control', 'Access Control', 'IMPLEMENTED', 'security-team'),
('AC-02', 'Multi-Factor Authentication', 'Authentication', 'IN_PROGRESS', 'identity-team'),
('CR-01', 'TLS 1.3 for Data in Transit', 'Cryptography', 'IMPLEMENTED', 'platform-team'),
('IN-01', 'Parameterized SQL Queries', 'Injection Prevention', 'IMPLEMENTED', 'app-team'),
('LG-01', 'Centralized Security Logging', 'Monitoring', 'PLANNED', 'sre-team');
