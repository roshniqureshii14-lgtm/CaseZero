package com.casezero.casezero;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Collects public article metadata from independent Pakistani publisher RSS feeds.
 * Articles are research leads only; they are not verified CASEZERO cases.
 */
@Service
public class NewsResearchService {
    private static final Logger log = LoggerFactory.getLogger(NewsResearchService.class);
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 12_000;
    private static final Pattern QUOTED_TERM = Pattern.compile("\"([^\"]+)\"");

    private static final List<FeedSource> SOURCES = List.of(
            new FeedSource("Dawn", "https://www.dawn.com/feeds/pakistan/"),
            new FeedSource("The Express Tribune", "https://tribune.com.pk/feed/home"),
            new FeedSource("The Nation", "https://nation.com.pk/rss/top-stories"),
            new FeedSource("Jang", "https://jang.com.pk/rss/1/1"),
            new FeedSource("The News International", "https://www.thenews.com.pk/rss/1/1"),
            new FeedSource("UrduPoint", "https://www.urdupoint.com/rss/urdupoint.rss"),
            new FeedSource("Express Urdu", "https://www.express.pk/feed/")
    );

    public List<ResearchArticle> searchPakistanNews(String searchTerm) {
        List<String> terms = extractTerms(searchTerm);
        Map<String, ResearchArticle> uniqueArticles = new LinkedHashMap<>();
        int successfulFeeds = 0;

        for (FeedSource source : SOURCES) {
            try {
                List<ResearchArticle> feedArticles = readFeed(source);
                successfulFeeds++;
                for (ResearchArticle article : feedArticles) {
                    String searchableText = (article.title() + " " + article.description())
                            .toLowerCase(Locale.ROOT);
                    if (terms.stream().anyMatch(searchableText::contains)) {
                        uniqueArticles.putIfAbsent(article.url(), article);
                    }
                }
            } catch (Exception ex) {
                // One unavailable publisher must not prevent collection from other feeds.
                log.warn("Could not read {} RSS feed: {}", source.name(), ex.getMessage());
            }
        }

        if (successfulFeeds == 0) {
            throw new IllegalStateException("All configured Pakistani publisher feeds were unavailable.");
        }
        log.info("RSS search matched {} unique article(s); {}/{} publisher feeds responded.",
                uniqueArticles.size(), successfulFeeds, SOURCES.size());
        return new ArrayList<>(uniqueArticles.values());
    }

    private List<ResearchArticle> readFeed(FeedSource source) throws Exception {
        URLConnection connection = new URL(source.url()).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "CASEZERO-PublicResearch/1.0 (RSS reader)");
        connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml");

        try (InputStream input = connection.getInputStream();
             XmlReader reader = new XmlReader(input)) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            List<ResearchArticle> articles = new ArrayList<>();

            for (SyndEntry entry : feed.getEntries()) {
                String title = cleanText(entry.getTitle());
                String link = entry.getLink() == null ? "" : entry.getLink().trim();
                String description = entry.getDescription() == null
                        ? "" : cleanText(entry.getDescription().getValue());
                Date date = entry.getPublishedDate() != null
                        ? entry.getPublishedDate() : entry.getUpdatedDate();
                String publishedAt = date == null ? "" : Instant.ofEpochMilli(date.getTime()).toString();

                if (!title.isBlank() && link.startsWith("https://")) {
                    articles.add(new ResearchArticle(title, link, source.name(), publishedAt, description));
                }
            }
            return articles;
        }
    }

    private List<String> extractTerms(String searchTerm) {
        List<String> terms = new ArrayList<>();
        if (searchTerm != null) {
            Matcher matcher = QUOTED_TERM.matcher(searchTerm);
            while (matcher.find()) {
                String term = matcher.group(1).trim().toLowerCase(Locale.ROOT);
                if (!term.isBlank()) terms.add(term);
            }
            if (terms.isEmpty() && !searchTerm.isBlank()) {
                terms.add(searchTerm.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (terms.isEmpty()) terms.add("missing person");
        return terms;
    }

    private String cleanText(String value) {
        if (value == null) return "";
        return value.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record FeedSource(String name, String url) {}
}