package com.casezero.casezero;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NewsResearchService {

    private static final String API_URL =
            "https://api.gdeltproject.org/api/v2/doc/doc";

    private final RestClient restClient = RestClient.builder()
            .defaultHeader("User-Agent", "CASEZERO-PublicResearch/1.0")
            .defaultHeader("Accept", "application/json")
            .build();
    private final ObjectMapper objectMapper;

    public NewsResearchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ResearchArticle> searchPakistanNews(String searchTerm) {
        String cleanTerm = searchTerm == null ? "" : searchTerm.trim();
        if (cleanTerm.isBlank()) {
            cleanTerm = "missing persons";
        }

        // Keep the feed Pakistan-focused while allowing the visitor to choose a topic.
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

        String responseBody = restClient.get()
                .uri(requestUrl)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("The news service returned an empty response.");
        }

        final JsonNode response;
        try {
            response = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException ex) {
            // GDELT can return a plain-text/HTML error page instead of JSON.
            throw new IllegalStateException("The news service did not return JSON data.", ex);
        }

        if (response == null || !response.path("articles").isArray()) {
            throw new IllegalStateException("The news service response did not contain an article list.");
        }

        List<ResearchArticle> articles = new ArrayList<>();
        for (JsonNode item : response.path("articles")) {
            String title = item.path("title").asText("");
            String url = item.path("url").asText("");
            String source = item.path("domain").asText("News source");
            String publishedAt = item.path("seendate").asText("");

            if (!title.isBlank() && url.startsWith("https://")) {
                articles.add(new ResearchArticle(title, url, source, publishedAt));
            }
        }
        return articles;
    }
}
