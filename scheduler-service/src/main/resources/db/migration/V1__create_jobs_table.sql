CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    job_type VARCHAR(80) NOT NULL,
    priority INTEGER NOT NULL,
    payload JSONB NOT NULL,
    estimated_duration_ms BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_worker_id VARCHAR(120),
    retry_count INTEGER NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    queued_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT jobs_priority_range CHECK (priority BETWEEN 0 AND 1000),
    CONSTRAINT jobs_estimated_duration_positive CHECK (estimated_duration_ms > 0),
    CONSTRAINT jobs_retry_count_non_negative CHECK (retry_count >= 0)
);

CREATE UNIQUE INDEX ux_jobs_idempotency_key
    ON jobs (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX ix_jobs_status_created_at ON jobs (status, created_at DESC);
CREATE INDEX ix_jobs_created_at ON jobs (created_at DESC);
