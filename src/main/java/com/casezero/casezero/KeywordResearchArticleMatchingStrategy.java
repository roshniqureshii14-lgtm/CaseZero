package com.casezero.casezero;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class KeywordResearchArticleMatchingStrategy implements ResearchArticleMatchingStrategy {

    @Override
    public boolean matches(ResearchArticle article, Set<String> searchTerms) {
        String searchableText = normalize(article.title() + " "
                + article.description() + " " + article.source());
        return searchTerms.stream().anyMatch(searchableText::contains);
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
}