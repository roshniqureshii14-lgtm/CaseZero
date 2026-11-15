-- Run once in Supabase SQL Editor if case_research_reports already exists.
ALTER TABLE public.case_research_reports
    ADD COLUMN IF NOT EXISTS description TEXT;