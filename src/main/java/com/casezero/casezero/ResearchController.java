package com.casezero.casezero;

import java.util.List;

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

    public ResearchController(NewsResearchService researchService) {
        this.researchService = researchService;
    }

    @GetMapping("/research")
    public String research(
            @RequestParam(required = false) String q,
            Model model) {

        boolean searchRequested = q != null && !q.isBlank();
        String searchTerm = searchRequested ? q.trim() : "";

        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("searchPerformed", searchRequested);
        model.addAttribute("articles", List.of());

        // Do not contact GDELT just because someone opens the Research Feed.
        // Only send an API request after the visitor submits a search.
        if (searchRequested) {
            try {
                model.addAttribute("articles", researchService.searchPakistanNews(searchTerm));
            } catch (Exception ex) {
                log.warn("Research Feed request failed: {}", ex.getMessage());
                model.addAttribute(
                        "apiError",
                        "External news search is temporarily unavailable. "
                        + "Please try again later. Existing CASEZERO case records are still available.");
            }
        }

        return "research";
    }
}
