# CASEZERO Multi-Source Case Discovery

## What it does
CASEZERO reads public RSS/Atom feeds from multiple Pakistani publishers, filters article titles and descriptions for unresolved-case keywords, removes repeated source URLs, and stores matching article metadata in the private `case_research_reports` table. These are research leads, not verified cases. Administrators must open the original source and review each report before creating a public case record.

## Configured feeds
- Dawn Pakistan: https://www.dawn.com/feeds/pakistan/
- The Express Tribune: https://tribune.com.pk/feed/home
- The Nation: https://nation.com.pk/rss/top-stories
- Jang: https://jang.com.pk/rss/1/1
- The News International: https://www.thenews.com.pk/rss/1/1
- UrduPoint: https://www.urdupoint.com/rss/urdupoint.rss
- Express Urdu: https://www.express.pk/feed/

Feeds are independent. A failed feed is logged and skipped so other publishers can still be processed. Publishers may change, restrict, or discontinue feeds; monitor logs and update the source list when necessary.

## Collection schedule
The first automatic collection starts one minute after application startup. Further collections run six hours after the previous run completes. An administrator can also trigger a collection from `/admin/research`.

## Database setup
For a new database, run `database/create_case_research_reports.sql` in Supabase SQL Editor. If the table already exists from the previous version, run `database/add_case_research_description.sql` to add the article description column. Keep the table private with RLS enabled and no public policies.

## Search and duplicate handling
The collector checks article titles and descriptions against English and Urdu keywords for missing persons, unidentified bodies, abduction, unsolved crimes and related topics. Exact source URLs are deduplicated in the database. Reports with the same title are also skipped by the application to reduce syndicated duplicates.

## Important limitations
- RSS normally exposes recent publisher-selected articles, not a complete historical archive. It cannot guarantee every historic Pakistani unresolved case will be discovered.
- Some publishers may expose only a limited number of recent entries.
- RSS feeds provide metadata and excerpts, not necessarily the full article.
- A headline match is not proof that an event is a cold case or remains unresolved.
- Never publish an allegation as fact. Verify the original report and record only source-supported details.
- Respect each publisher's terms and copyright. CASEZERO stores short metadata and links to original reporting rather than republishing full articles.

## Local verification
1. Pull branch `feature/multi-source-case-discovery`.
2. Run `.\mvnw.cmd clean test`.
3. Start with `.\mvnw.cmd spring-boot:run`.
4. Sign in with an account whose role is ADMIN.
5. Open `http://localhost:8080/admin/research` and use the collection button.
6. Confirm that reports from different publisher names appear in the pending queue and in Supabase.
7. Run collection again and confirm that the same source URLs are not inserted twice.
8. Open `http://localhost:8080/research?q=missing%20person` to inspect public search results.
