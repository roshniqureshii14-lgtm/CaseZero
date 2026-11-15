package com.casezero.casezero;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NewsResearchService {

    private static final Logger log = LoggerFactory.getLogger(NewsResearchService.class);
    private static final String API_URL =
            "https://api.gdeltproject.org/api/v2/doc/doc";

    // GDELT asks clients to make no more than one request every five seconds.
    private static final long MIN_REQUEST_INTERVAL_MS = 5_300;
    private static final long CACHE_TTL_MS = 10 * 60 * 1_000L;
    private static final long STALE_CACHE_TTL_MS = 24 * 60 * 60 * 1_000L;
    private static final long RATE_LIMIT_COOLDOWN_MS = 60 * 1_000L;

    private final RestClient restClient = RestClient.builder()
            .defaultHeader("User-Agent", "CASEZERO-PublicResearch/1.0")
            .defaultHeader("Accept", "application/json")
            .build();
    private final ObjectMapper objectMapper;
    private final Map<String, CachedArticles> cache = new ConcurrentHashMap<>();
    private final Object requestLock = new Object();

    private long lastRequestStartedAt;
    private long rateLimitedUntil;

    public NewsResearchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ResearchArticle> searchPakistanNews(String searchTerm) {
        String cleanTerm = searchTerm == null ? "" : searchTerm.trim();
        if (cleanTerm.isBlank()) {
            cleanTerm = "missing persons";
        }

        String cacheKey = cleanTerm.toLowerCase();
        CachedArticles cached = cache.get(cacheKey);
        long now = System.currentTimeMillis();

        if (cached != null && now - cached.fetchedAt < CACHE_TTL_MS) {
            log.debug("Serving Research Feed results from cache for '{}'.", cleanTerm);
            return cached.articles;
        }

        // Serialize outbound requests so different searches also respect GDELT's limit.
        synchronized (requestLock) {
            cached = cache.get(cacheKey);
            now = System.currentTimeMillis();

            if (cached != null && now - cached.fetchedAt < CACHE_TTL_MS) {
                return cached.articles;
            }

            if (now < rateLimitedUntil) {
                return useStaleCacheOrThrow(cacheKey, cleanTerm,
                        "GDELT has temporarily rate-limited CASEZERO. Please try again later.");
            }

            long waitMillis = MIN_REQUEST_INTERVAL_MS - (now - lastRequestStartedAt);
            if (lastRequestStartedAt > 0 && waitMillis > 0) {
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return useStaleCacheOrThrow(cacheKey, cleanTerm,
                            "The Research Feed request was interrupted.");
                }
            }

            lastRequestStartedAt = System.currentTimeMillis();

            try {
                List<ResearchArticle> articles = fetchFromGdelt(cleanTerm);
                List<ResearchArticle> safeArticles = List.copyOf(articles);
                cache.put(cacheKey, new CachedArticles(safeArticles, System.currentTimeMillis()));
                return safeArticles;
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value() == 429) {
                    rateLimitedUntil = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS;
                    log.warn("GDELT rate limit reached. CASEZERO will pause new requests for 60 seconds.");
                }
                return useStaleCacheOrThrow(cacheKey, cleanTerm,
                        "The GDELT service returned HTTP " + ex.getStatusCode().value()
                                + ". Response preview: " + responsePreview(ex.getResponseBodyAsString()));
            } catch (RestClientException | IllegalStateException ex) {
                return useStaleCacheOrThrow(cacheKey, cleanTerm,
                        "The GDELT request could not be completed: " + ex.getMessage());
            }
        }
    }

    private List<ResearchArticle> fetchFromGdelt(String cleanTerm) {
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

        ResponseEntity<String> apiResponse = restClient.get()
                .uri(requestUrl)
                .retrieve()
                .toEntity(String.class);

        String contentType = apiResponse.getHeaders().getFirst("Content-Type");
        String responseBody = apiResponse.getBody();

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("The news service returned an empty response. Content-Type: "
                    + contentType);
        }

        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).contains("json")) {
            throw new IllegalStateException("GDELT returned a non-JSON response. Content-Type: "
                    + contentType + ". Response preview: " + responsePreview(responseBody));
        }

        final JsonNode response;
        try {
            response = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException ex) {
            // GDELT can return a plain-text/HTML error page instead of JSON.
            throw new IllegalStateException("The news service did not return JSON data.", ex);
        }

        if (response == null || !response.path("articles").isArray()) {
            throw new IllegalStateException("The news service response did not contain an article list. "
                    + "Content-Type: " + contentType + ". Response preview: " + responsePreview(responseBody));
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

    private String responsePreview(String body) {
        if (body == null || body.isBlank()) {
            return "(empty response body)";
        }
        String oneLine = body.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 500 ? oneLine : oneLine.substring(0, 500) + "...";
    }

    private List<ResearchArticle> useStaleCacheOrThrow(
            String cacheKey, String searchTerm, String reason) {
        CachedArticles cached = cache.get(cacheKey);
        long age = cached == null ? Long.MAX_VALUE
                : System.currentTimeMillis() - cached.fetchedAt;

        if (cached != null && age <= STALE_CACHE_TTL_MS) {
            log.warn("Using saved Research Feed results for '{}' because: {}", searchTerm, reason);
            return cached.articles;
        }

        throw new IllegalStateException(reason);
    }

    private static final class CachedArticles {
        private final List<ResearchArticle> articles;
        private final long fetchedAt;

        private CachedArticles(List<ResearchArticle> articles, long fetchedAt) {
            this.articles = articles;
            this.fetchedAt = fetchedAt;
        }
    }
}
