package com.casezero.casezero;

/**
 * A news article returned by the GDELT research API.
 * These links are research leads, not verified CASEZERO case records.
 */
public record ResearchArticle(
        String title,
        String url,
        String source,
        String publishedAt
) {
}
