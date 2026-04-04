-- Add status column to subscription_plan
ALTER TABLE subscription_plan
ADD COLUMN IF NOT EXISTS status BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE subscription_plan
SET status = TRUE
WHERE status IS NULL;
-- Add status column to subscription_usage_history
ALTER TABLE subscription_usage_history
ADD COLUMN IF NOT EXISTS status BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE subscription_usage_history
SET status = TRUE
WHERE status IS NULL;