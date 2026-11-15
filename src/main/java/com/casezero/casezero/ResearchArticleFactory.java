package com.casezero.casezero;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.rometools.rome.feed.synd.SyndEntry;

/**
 * Factory for converting publisher RSS entries into validated CASEZERO
 * research article records.
 */
@Component
public class ResearchArticleFactory {

    public Optional<ResearchArticle> from(SyndEntry entry, String sourceName) {
        if (entry == null) return Optional.empty();

        String title = cleanText(entry.getTitle());
        String url = entry.getLink() == null ? "" : entry.getLink().trim();
        String description = entry.getDescription() == null
                ? "" : cleanText(entry.getDescription().getValue());

        Date date = entry.getPublishedDate() != null
                ? entry.getPublishedDate() : entry.getUpdatedDate();
        String publishedAt = date == null
                ? "" : Instant.ofEpochMilli(date.getTime()).toString();

        // Only usable titles and HTTPS publisher links become research leads.
        if (title.isBlank() || !url.startsWith("https://")) {
            return Optional.empty();
        }

        return Optional.of(new ResearchArticle(
                title, url, sourceName, publishedAt, description));
    }

    private String cleanText(String value) {
        if (value == null) return "";
        return value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }
}