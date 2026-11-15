package com.casezero.casezero;

import java.util.Set;

/**
 * Strategy contract for matching public research articles to a search query.
 * Alternative matching strategies can be added without changing the feed service.
 */
public interface ResearchArticleMatchingStrategy {
    boolean matches(ResearchArticle article, Set<String> searchTerms);
}