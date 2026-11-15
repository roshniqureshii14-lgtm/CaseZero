-- CASEZERO unique usernames
-- Run once in the Supabase SQL Editor BEFORE starting the updated application.
-- Existing accounts receive stable placeholder usernames; no login/email data changes.

ALTER TABLE public.app_users
    ADD COLUMN IF NOT EXISTS username VARCHAR(24);

UPDATE public.app_users
SET username = 'member_' || id::text
WHERE username IS NULL OR BTRIM(username) = '';

ALTER TABLE public.app_users
    ALTER COLUMN username SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_username_lower
    ON public.app_users (LOWER(username));
