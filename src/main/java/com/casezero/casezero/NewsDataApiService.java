package com.casezero.casezero;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Optional external news-search API. RSS collection remains fully usable when
 * no API key is configured.
 */
@Service
public class NewsDataApiService {
    private static final Logger log = LoggerFactory.getLogger(NewsDataApiService.class);
    private static final String API_URL = "https://newsdata.io/api/1/latest";
    private static final String CASE_QUERY =
            "\"missing person\" OR \"missing persons\" OR \"missing girl\" OR \"missing boy\" OR "
            + "\"unidentified body\" OR \"unidentified persons\" OR \"unclaimed body\" OR "
            + "\"unsolved murder\" OR \"unsolved crime\" OR \"cold case\" OR "
            + "\"kidnapped\" OR \"abducted\" OR \"لاپتہ\" OR \"گمشدہ\" OR "
            + "\"نامعلوم لاش\" OR \"اغوا\" OR \"قتل\"";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;

    public NewsDataApiService(
            ObjectMapper objectMapper,
            @Value("${CASEZERO_NEWSDATA_API_KEY:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "CASEZERO-PublicResearch/1.0")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public List<ResearchArticle> collectPakistanReports() {
        if (apiKey.isBlank()) {
            log.info("NewsData.io is not configured; continuing with publisher RSS feeds.");
            return List.of();
        }

        try {
            String requestUrl = UriComponentsBuilder.fromUriString(API_URL)
                    .queryParam("apikey", apiKey)
                    .queryParam("q", CASE_QUERY)
                    .queryParam("country", "pk")
                    .queryParam("language", "en,ur")
                    .queryParam("size", 10)
                    .queryParam("removeduplicate", 1)
                    .build()
                    .encode()
                    .toUriString();

            String body = restClient.get().uri(requestUrl).retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("NewsData.io returned an empty response.");
            }

            JsonNode root = objectMapper.readTree(body);
            if (!"success".equalsIgnoreCase(root.path("status").asText())) {
                throw new IllegalStateException("NewsData.io reported: "
                        + root.path("message").asText("unknown API error"));
            }

            List<ResearchArticle> articles = new ArrayList<>();
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode item : results) {
                    String title = item.path("title").asText("");
                    String link = item.path("link").asText("");
                    String source = item.path("source_name").asText("NewsData.io publisher");
                    String description = item.path("description").asText("");
                    String publishedAt = item.path("pubDate").asText("");
                    if (!title.isBlank() && link.startsWith("https://")) {
                        articles.add(new ResearchArticle(title, link, source, publishedAt, description));
                    }
                }
            }
            log.info("NewsData.io returned {} Pakistan-related article(s).", articles.size());
            return articles;
        } catch (Exception ex) {
            // The API is an optional additional source; RSS results remain usable.
            log.warn("NewsData.io collection failed; RSS collection will continue: {}", ex.getMessage());
            return List.of();
        }
    }
}