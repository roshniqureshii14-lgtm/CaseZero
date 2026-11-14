package com.casezero.casezero;

/**
 * Public article metadata used as a CASEZERO research lead.
 * It is not, by itself, a verified CASEZERO case.
 */
public record ResearchArticle(
        String title,
        String url,
        String source,
        String publishedAt,
        String description
) {
    public ResearchArticle(String title, String url, String source, String publishedAt) {
        this(title, url, source, publishedAt, "");
    }
}