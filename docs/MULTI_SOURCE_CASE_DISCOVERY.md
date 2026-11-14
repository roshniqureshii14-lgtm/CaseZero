# CASEZERO Multi-Source Case Discovery

## What it does
CASEZERO reads public RSS/Atom feeds from multiple Pakistani publishers, optionally supplements them with the NewsData.io REST API, filters article titles and descriptions for unresolved-case keywords, removes repeated source URLs and titles, and stores matching article metadata in the private `case_research_reports` table. These are research leads, not verified cases. Administrators must open the original source and review each report before creating a public case record.

## Configured publisher feeds
- Dawn Pakistan: https://www.dawn.com/feeds/pakistan/
- The Express Tribune: https://tribune.com.pk/feed/home
- The Nation: https://nation.com.pk/rss/top-stories
- Jang: https://jang.com.pk/rss/1/1
- The News International: https://www.thenews.com.pk/rss/1/1
- UrduPoint: https://www.urdupoint.com/rss/urdupoint.rss
- Express Urdu: https://www.express.pk/feed/

Each feed is read independently. A failed publisher is logged and skipped so other publishers can still be processed.

## Optional news API
The collector also supports NewsData.io. It is disabled safely until an API key is supplied as the environment variable `CASEZERO_NEWSDATA_API_KEY`. Never paste the key into source code or commit it to GitHub.

NewsData.io documents a free tier of 200 credits per day, up to 10 articles per request, with latest-news data delayed by 12 hours and limited to the previous 48 hours. The collector makes one API request per collection run and asks for Pakistan, English/Urdu and unresolved-case keywords. Free-plan limits and availability can change; check the provider's current documentation before relying on it.

## Collection schedule
The first automatic collection starts one minute after application startup. Further collections run six hours after the previous run completes. An administrator can also trigger collection from `/admin/research`.

## Database setup
For a new database, run `database/create_case_research_reports.sql` in Supabase SQL Editor. If the table already exists from the previous version, run `database/add_case_research_description.sql` to add the article description column. Keep the table private with RLS enabled and no public policies.

## Search and duplicate handling
The collector checks titles and descriptions against English and Urdu keywords for missing persons, unidentified bodies, abduction, unsolved crimes and related topics. Exact source URLs and case-insensitive article titles are deduplicated before insertion.

## Important limitations
- Publisher RSS feeds mostly expose recent publisher-selected articles, not complete historical archives.
- NewsData.io's free latest-news tier only covers the previous 48 hours and is delayed; it is not a complete historical case archive.
- Some publishers may change, restrict or discontinue feeds.
- RSS/API results are leads. A headline match does not prove that an event is a cold case or remains unresolved.
- Never publish allegations as facts. Verify the original report and record only source-supported details.
- Respect each publisher's terms and copyright. CASEZERO stores short metadata and links to original reporting rather than republishing full articles.

## Local verification
1. Pull branch `feature/multi-source-case-discovery`.
2. Run `.\mvnw.cmd clean test`.
3. Start with `.\mvnw.cmd spring-boot:run`.
4. Sign in with an account whose role is ADMIN.
5. Open `http://localhost:8080/admin/research` and use the collection button.
6. Confirm reports from different publisher names appear in the pending queue and in Supabase.
7. Run collection again and confirm that the same source URLs/titles are not inserted twice.
8. Open `http://localhost:8080/research?q=missing%20person` to inspect public publisher-feed search results.
