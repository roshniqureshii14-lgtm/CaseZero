package com.casezero.casezero;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "about", "case", "cases", "for", "from",
            "in", "is", "of", "on", "the", "to", "with", "pakistan", "pakistani");

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
        Set<String> terms = extractTerms(searchTerm);
        Map<String, ResearchArticle> uniqueArticles = new LinkedHashMap<>();
        int successfulFeeds = 0;

        for (FeedSource source : SOURCES) {
            try {
                List<ResearchArticle> feedArticles = readFeed(source);
                successfulFeeds++;
                for (ResearchArticle article : feedArticles) {
                    String searchableText = normalize(article.title() + " "
                            + article.description() + " " + article.source());
                    if (terms.stream().anyMatch(searchableText::contains)) {
                        uniqueArticles.putIfAbsent(article.url(), article);
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not read {} RSS feed: {}", source.name(), ex.getMessage());
            }
        }

        if (successfulFeeds == 0) {
            throw new IllegalStateException("All configured Pakistani publisher feeds were unavailable.");
        }
        log.info("RSS search for [{}] matched {} unique article(s); {}/{} publisher feeds responded.",
                searchTerm, uniqueArticles.size(), successfulFeeds, SOURCES.size());
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

    /**
     * Treats unquoted multi-word searches as separate keywords rather than one
     * impossible exact phrase. Quoted phrases remain supported. Publisher names
     * are searchable too, so searches such as "Jang" can find that feed's articles.
     */
    private Set<String> extractTerms(String searchTerm) {
        Set<String> terms = new LinkedHashSet<>();
        if (searchTerm == null || searchTerm.isBlank()) {
            terms.add("missing");
            return expandTerms(terms);
        }

        Matcher quoted = QUOTED_TERM.matcher(searchTerm);
        StringBuilder unquoted = new StringBuilder(searchTerm);
        while (quoted.find()) {
            String phrase = normalize(quoted.group(1));
            if (!phrase.isBlank()) terms.add(phrase);
            int start = quoted.start();
            int end = quoted.end();
            for (int i = start; i < end; i++) unquoted.setCharAt(i, ' ');
        }

        Matcher words = WORD.matcher(normalize(unquoted.toString()));
        while (words.find()) {
            String word = words.group();
            if (!STOP_WORDS.contains(word) && word.length() > 1) {
                terms.add(word);
            }
        }
        if (terms.isEmpty()) terms.add(normalize(searchTerm));
        return expandTerms(terms);
    }

    private Set<String> expandTerms(Set<String> original) {
        Set<String> expanded = new LinkedHashSet<>(original);
        for (String term : original) {
            switch (term) {
                case "missing", "missed", "لاپتہ", "گمشدہ" ->
                        expanded.addAll(List.of("missing person", "missing persons",
                                "missing girl", "missing boy", "لاپتہ", "گمشدہ"));
                case "unsolved", "cold" ->
                        expanded.addAll(List.of("unsolved crime", "unsolved murder",
                                "cold case", "murder", "قتل"));
                case "unidentified" ->
                        expanded.addAll(List.of("unidentified person", "unidentified persons",
                                "unidentified body", "نامعلوم لاش"));
                case "kidnap", "kidnapped", "abducted" ->
                        expanded.addAll(List.of("kidnapped", "abducted", "اغوا"));
                case "murder", "قتل" ->
                        expanded.addAll(List.of("murder", "قتل", "unsolved murder"));
                default -> { }
            }
        }
        return expanded;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanText(String value) {
        return normalize(value);
    }

    private record FeedSource(String name, String url) {}
}
