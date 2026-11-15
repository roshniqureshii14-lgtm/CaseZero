-- CASEZERO profile avatar support
-- Run once in Supabase SQL Editor after applying profile_and_following.sql.

ALTER TABLE public.app_users
    ADD COLUMN IF NOT EXISTS avatar_key VARCHAR(40) NOT NULL DEFAULT 'case-file';

UPDATE public.app_users
SET avatar_key = 'case-file'
WHERE avatar_key IS NULL OR BTRIM(avatar_key) = '';

