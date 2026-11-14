# CASEZERO API case-intake setup

## What this feature does
CASEZERO uses its existing GDELT DOC API integration to discover recent Pakistan-related
news reports. Results are saved in the private `case_research_reports` table.
They are not public cases until an administrator checks the source, supplies a
supported category/location/neutral summary, and approves the record.

## 1. Create the database table
1. Open the CASEZERO Supabase project and go to SQL Editor.
2. Open a new query.
3. Copy and run `database/create_case_research_reports.sql`.
4. Confirm `case_research_reports` exists in the public schema.
Do not expose this table through the public Supabase Data API. Spring Boot uses its
existing PostgreSQL connection.

## 2. Assign your account administrator access
First register and sign in with the account you want to administer. In Supabase SQL
Editor, replace the email below with that account's registered email and run:

```sql
UPDATE public.app_users
SET role = 'ADMIN'
WHERE LOWER(email) = LOWER('YOUR_REGISTERED_EMAIL');
```

Verify the role:
```sql
SELECT name, email, role FROM public.app_users
WHERE LOWER(email) = LOWER('YOUR_REGISTERED_EMAIL');
```
Only ADMIN accounts can use the review page. Normal accounts remain USER.

## 3. Run the application
From the CASEZERO folder in VS Code PowerShell:
```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```
Sign in with the administrator account and open `http://localhost:8080/admin/research`.
Click **Collect new reports from GDELT** to request a recent batch.

## 4. Review and publish
Open each original article, check that it concerns a Pakistan-related unresolved case,
select its category, enter a supported location and neutral summary, then approve or
reject it. Approval creates a row in the existing `case_records` table. Rejected
reports never appear in the public archive.

## Limitations and responsible use
- GDELT article lists cover a rolling recent-news window (up to three months); they
  are not a complete historical cold-case database.
- Collection is manual and on-demand in this version, not a background scheduler.
- API results are research leads, not proof of allegations or current missing status.
- Duplicate source URLs are skipped; separate reports about the same incident may
  still need human duplicate review.
