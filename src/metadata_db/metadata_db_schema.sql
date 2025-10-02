-- MetadataDB schema for Android hybrid antivirus project
-- File: metadata_db_schema.sql
-- PostgreSQL SQL script
-- Creates types, tables, indexes, triggers and helper functions
-- Requires: PostgreSQL 12+ (recommended). Enable pgcrypto for gen_random_uuid()

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ENUM types
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'job_status') THEN
        CREATE TYPE job_status AS ENUM ('queued','static','dynamic','done','error');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'report_type') THEN
        CREATE TYPE report_type AS ENUM ('static','dynamic','hybrid');
    END IF;
END$$;

-- TABLE: jobs
CREATE TABLE IF NOT EXISTS jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id TEXT NOT NULL UNIQUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  current_status job_status NOT NULL DEFAULT 'queued',
  score NUMERIC(5,4) NULL,
  processing_time_ms INTEGER NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  error_message TEXT NULL,
  metadata JSONB NULL
);

CREATE INDEX IF NOT EXISTS idx_jobs_status_created ON jobs (current_status, created_at);
CREATE INDEX IF NOT EXISTS idx_jobs_jobid ON jobs (job_id);

-- Trigger to update updated_at automatically
CREATE OR REPLACE FUNCTION trg_update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS jobs_updated_at_trg ON jobs;
CREATE TRIGGER jobs_updated_at_trg
BEFORE UPDATE ON jobs
FOR EACH ROW
EXECUTE FUNCTION trg_update_updated_at();

-- TABLE: job_status_history
CREATE TABLE IF NOT EXISTS job_status_history (
  id BIGSERIAL PRIMARY KEY,
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  old_status job_status NULL,
  new_status job_status NOT NULL,
  changed_by TEXT NULL,
  reason TEXT NULL,
  ts TIMESTAMP WITH TIME ZONE DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_status_history_job ON job_status_history (job_id, ts);

-- TABLE: reports
CREATE TABLE IF NOT EXISTS reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  report_type report_type NOT NULL,
  score NUMERIC(5,4) NULL,
  report_json JSONB NOT NULL,
  generated_by TEXT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  size_bytes BIGINT NULL
);
CREATE INDEX IF NOT EXISTS idx_reports_job ON reports (job_id);
CREATE INDEX IF NOT EXISTS idx_reports_type ON reports (report_type);

-- TABLE: static_analysis
CREATE TABLE IF NOT EXISTS static_analysis (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID UNIQUE NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  apk_permissions JSONB NULL,
  api_calls JSONB NULL,
  suspicious_patterns JSONB NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- TABLE: dynamic_analysis
CREATE TABLE IF NOT EXISTS dynamic_analysis (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID UNIQUE NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  runtime_traces JSONB NULL,
  network_activity JSONB NULL,
  system_calls JSONB NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- TABLE: files
CREATE TABLE IF NOT EXISTS files (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  storage_type TEXT NOT NULL,
  path TEXT NOT NULL,
  file_type TEXT NULL,
  size_bytes BIGINT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_files_job ON files (job_id);

-- TABLE: hashes
CREATE TABLE IF NOT EXISTS hashes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  file_id UUID NULL REFERENCES files(id) ON DELETE SET NULL,
  algo TEXT NOT NULL,
  hash TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_hashes_job ON hashes (job_id);

-- TABLE: logs
CREATE TABLE IF NOT EXISTS logs (
  id BIGSERIAL PRIMARY KEY,
  job_id UUID NULL REFERENCES jobs(id) ON DELETE SET NULL,
  level TEXT NOT NULL,
  message TEXT NOT NULL,
  source TEXT NULL,
  ts TIMESTAMP WITH TIME ZONE DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_logs_job ON logs (job_id);

-- TABLE: metrics
CREATE TABLE IF NOT EXISTS metrics (
  id BIGSERIAL PRIMARY KEY,
  metric_name TEXT NOT NULL,
  metric_value NUMERIC NOT NULL,
  observed_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  note TEXT NULL
);
CREATE INDEX IF NOT EXISTS idx_metrics_name_at ON metrics (metric_name, observed_at DESC);

-- Optional: dependencies table to register versions of services
CREATE TABLE IF NOT EXISTS dependencies (
  id BIGSERIAL PRIMARY KEY,
  service_name TEXT NOT NULL,
  service_version TEXT NULL,
  added_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  note TEXT NULL
);

-- Helper function: compute p95 for processing_time_ms over a time window and insert metric
CREATE OR REPLACE FUNCTION record_processing_time_p95(interval_text TEXT DEFAULT '1 hour')
RETURNS TABLE(p95_value NUMERIC) LANGUAGE plpgsql AS $$
DECLARE
  q TEXT;
  p95 NUMERIC;
BEGIN
  q := format($f$
    SELECT percentile_disc(0.95) WITHIN GROUP (ORDER BY processing_time_ms) AS p95
    FROM jobs
    WHERE processing_time_ms IS NOT NULL AND created_at >= now() - interval '%s'
  $f$, interval_text);
  EXECUTE q INTO p95;
  IF p95 IS NOT NULL THEN
    INSERT INTO metrics(metric_name, metric_value, observed_at, note)
    VALUES ('processing_time_p95', p95, now(), 'p95 over ' || interval_text);
  END IF;
  RETURN QUERY SELECT p95;
END; $$;

-- Example of safe upsert for inserting/updating a job from an ingest payload
-- This demonstrates the pattern: lock row, update status, insert history
CREATE OR REPLACE FUNCTION upsert_job_result(p_job_id TEXT, p_status job_status, p_score NUMERIC DEFAULT NULL, p_processing_time_ms INTEGER DEFAULT NULL, p_changed_by TEXT DEFAULT 'ingest')
RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
  v_job_uuid UUID;
  v_old_status job_status;
BEGIN
  -- Find job
  SELECT id, current_status INTO v_job_uuid, v_old_status
  FROM jobs
  WHERE job_id = p_job_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'job not found: %', p_job_id;
  END IF;

  UPDATE jobs
  SET current_status = p_status,
      score = COALESCE(p_score, score),
      processing_time_ms = COALESCE(p_processing_time_ms, processing_time_ms),
      attempts = attempts + 1
  WHERE id = v_job_uuid;

  INSERT INTO job_status_history(job_id, old_status, new_status, changed_by, ts)
  VALUES (v_job_uuid, v_old_status, p_status, p_changed_by, now());

EXCEPTION WHEN others THEN
  -- Bubble up the error to caller; application can implement retry logic
  RAISE;
END; $$;

-- Sample function to mark job error with message (idempotent)
CREATE OR REPLACE FUNCTION mark_job_error(p_job_id TEXT, p_err_msg TEXT, p_changed_by TEXT DEFAULT 'system')
RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
  v_job_uuid UUID;
  v_old_status job_status;
BEGIN
  SELECT id, current_status INTO v_job_uuid, v_old_status FROM jobs WHERE job_id = p_job_id FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'job not found: %', p_job_id;
  END IF;

  UPDATE jobs SET current_status = 'error', error_message = p_err_msg, attempts = attempts + 1 WHERE id = v_job_uuid;
  INSERT INTO job_status_history(job_id, old_status, new_status, changed_by, reason, ts)
    VALUES (v_job_uuid, v_old_status, 'error', p_changed_by, p_err_msg, now());

EXCEPTION WHEN others THEN
  RAISE;
END; $$;

-- Grant minimal example privileges (adjust per your environment)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO your_app_role;

-- Example data (commented). Uncomment to insert sample row for testing.
-- INSERT INTO jobs(job_id, current_status, metadata) VALUES ('abc123', 'queued', '{"uploader":"user1"}');
-- SELECT * FROM jobs;
-- SELECT record_processing_time_p95('24 hours');

-- End of script
