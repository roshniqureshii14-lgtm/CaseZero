package com.casezero.casezero;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ResearchController {

    private static final Logger log = LoggerFactory.getLogger(ResearchController.class);
    private final NewsResearchService researchService;
    private final CaseResearchReportRepository reportRepository;

    public ResearchController(NewsResearchService researchService,
                              CaseResearchReportRepository reportRepository) {
        this.researchService = researchService;
        this.reportRepository = reportRepository;
    }

    @GetMapping("/research")
    public String research(@RequestParam(required = false) String q, Model model) {
        boolean searchRequested = q != null && !q.isBlank();
        String searchTerm = searchRequested ? q.trim() : "";

        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("searchPerformed", searchRequested);
        model.addAttribute("articles", List.of());

        if (searchRequested) {
            Map<String, ResearchArticle> combined = new LinkedHashMap<>();

            // Live publisher feeds provide newly published public reporting.
            try {
                for (ResearchArticle article : researchService.searchPakistanNews(searchTerm)) {
                    combined.putIfAbsent(article.url(), article);
                }
            } catch (Exception ex) {
                log.warn("Live Research Feed request failed: {}", ex.getMessage());
                model.addAttribute("liveFeedWarning",
                        "Some live publisher feeds could not be reached. Saved research leads are still searched below.");
            }

            // Include public-source leads already collected in Supabase. These are
            // still unverified reports, not automatically approved CASEZERO cases.
            try {
                List<String> visibleStatuses = List.of("PENDING", "APPROVED");
                for (CaseResearchReport report :
                        reportRepository.findAllByStatusInOrderByCollectedAtDesc(visibleStatuses)) {
                    String searchable = normalize(report.getTitle() + " "
                            + report.getDescription() + " " + report.getSourceName());
                    if (matches(searchable, searchTerm)) {
                        ResearchArticle saved = new ResearchArticle(
                                report.getTitle(), report.getSourceUrl(), report.getSourceName(),
                                report.getPublishedAt(), report.getDescription());
                        combined.putIfAbsent(saved.url(), saved);
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not search saved CASEZERO research reports: {}", ex.getMessage());
                model.addAttribute("savedFeedWarning",
                        "Saved reports could not be loaded from the database. Please check the database connection.");
            }

            model.addAttribute("articles", new ArrayList<>(combined.values()));
        }

        return "research";
    }

    private boolean matches(String searchableText, String query) {
        String normalizedText = normalize(searchableText);
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) return false;
        if (normalizedText.contains(normalizedQuery)) return true;

        // Match meaningful words separately. Generic words such as "case" are
        // ignored so "missing cases" can match "missing person".
        String[] words = normalizedQuery.split("[^\\p{L}\\p{N}]+");
        boolean hasMeaningfulWord = false;
        for (String word : words) {
            if (word.length() < 2 || List.of("the", "and", "for", "with", "from",
                    "case", "cases", "pakistan", "pakistani").contains(word)) {
                continue;
            }
            hasMeaningfulWord = true;
            if (normalizedText.contains(word)) return true;
            if (word.equals("missing") && (normalizedText.contains("لاپتہ")
                    || normalizedText.contains("گمشدہ"))) return true;
            if (word.equals("unsolved") && (normalizedText.contains("cold case")
                    || normalizedText.contains("قتل"))) return true;
            if (word.equals("unidentified") && normalizedText.contains("نامعلوم")) return true;
        }
        return !hasMeaningfulWord && normalizedText.contains(normalizedQuery);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
