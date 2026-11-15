package com.casezero.casezero;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Safety screening for member-submitted investigation text.
 *
 * This service does not verify facts, identify suspects, or determine guilt.
 * Clean content is only eligible for automatic publication when source notes
 * are present and no obvious private contact/identity details are detected.
 */
@Service
public class AiInvestigationModerationService {

    private static final Pattern EMAIL =
            Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PAKISTAN_PHONE =
            Pattern.compile("(?<!\\d)(?:\\+92|0092|0)3\\d{2}[- ]?\\d{7}(?!\\d)");
    private static final Pattern CNIC =
            Pattern.compile("(?<!\\d)\\d{5}-?\\d{7}-?\\d(?!\\d)");

    private final String apiKey;
    private final RestClient restClient;

    public AiInvestigationModerationService(
            @Value("${CASEZERO_OPENAI_API_KEY:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.restClient = RestClient.create("https://api.openai.com");
    }

    public ModerationDecision moderate(InvestigationDraft draft) {
        String combined = safe(draft.getTitle()) + "\n\n"
                + safe(draft.getFindings()) + "\n\n"
                + safe(draft.getSourceNotes());

        // Do not automatically publish submissions without references.
        if (safe(draft.getSourceNotes()).isBlank()) {
            return review("Please add source notes before this investigation can be automatically approved.");
        }

        // The text moderation endpoint is not a privacy/PII detector.
        // Keep obvious contact numbers, emails and CNIC-like identifiers out of auto-publication.
        if (EMAIL.matcher(combined).find()
                || PAKISTAN_PHONE.matcher(combined).find()
                || CNIC.matcher(combined).find()) {
            return review("The submission may contain private contact or identity information.");
        }

        if (apiKey.isBlank()) {
            return review("AI moderation is not configured yet. The submission has been kept private until the API key is added.");
        }

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/moderations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .body(java.util.Map.of(
                            "model", "omni-moderation-latest",
                            "input", combined))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode result = response == null ? null
                    : response.path("results").path(0);
            if (result == null || result.isMissingNode()) {
                return review("The AI moderation service returned an unreadable result.");
            }

            boolean flagged = result.path("flagged").asBoolean(true);
            if (flagged) {
                return review("The AI moderator flagged this submission for safety review.");
            }

            return new ModerationDecision(
                    "APPROVED",
                    "AI safety screening passed. Your sourced community investigation is now visible on its case page. This screening does not verify the truth of the claims.");

        } catch (RestClientException | IllegalArgumentException ex) {
            // Fail closed: service/network/API errors must never publish unmoderated text.
            return review("AI moderation could not be completed. Your submission remains private for review.");
        }
    }

    private ModerationDecision review(String message) {
        return new ModerationDecision("PENDING_REVIEW", message);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record ModerationDecision(String status, String memberMessage) {
    }
}
