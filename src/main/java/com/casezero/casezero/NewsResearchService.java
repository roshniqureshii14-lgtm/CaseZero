package com.casezero.casezero;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NewsResearchService {

    private static final String API_URL =
            "https://api.gdeltproject.org/api/v2/doc/doc";

    private final RestClient restClient = RestClient.create();

    public List<ResearchArticle> searchPakistanNews(String searchTerm) {

        String cleanTerm = searchTerm == null ? "" : searchTerm.trim();
        if (cleanTerm.isBlank()) {
            cleanTerm = "missing persons";
        }

        // Keep the search Pakistan-focused while allowing a user-selected topic.
        String query = "Pakistan " + cleanTerm;

        String requestUrl = UriComponentsBuilder
                .fromUriString(API_URL)
                .queryParam("query", query)
                .queryParam("mode", "artlist")
                .queryParam("format", "json")
                .queryParam("timespan", "3months")
                .queryParam("maxrecords", 15)
                .build()
                .encode()
                .toUriString();

        JsonNode response = restClient.get()
                .uri(requestUrl)
                .retrieve()
                .body(JsonNode.class);

        List<ResearchArticle> articles = new ArrayList<>();

        if (response == null || !response.path("articles").isArray()) {
            return articles;
        }

        for (JsonNode item : response.path("articles")) {
            String title = item.path("title").asText("");
            String url = item.path("url").asText("");
            String source = item.path("domain").asText("News source");
            String publishedAt = item.path("seendate").asText("");

            if (!title.isBlank() && url.startsWith("http")) {
                articles.add(new ResearchArticle(
                        title, url, source, publishedAt));
            }
        }

        return articles;
    }
}
