package com.casezero.casezero;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

@Controller
public class ResearchController {

    private final NewsResearchService researchService;

    public ResearchController(NewsResearchService researchService) {
        this.researchService = researchService;
    }

    @GetMapping("/research")
    public String research(
            @RequestParam(defaultValue = "missing persons") String q,
            Model model) {

        String searchTerm = q.trim();
        if (searchTerm.isBlank()) {
            searchTerm = "missing persons";
        }

        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("articles", List.of());

        try {
            model.addAttribute(
                    "articles",
                    researchService.searchPakistanNews(searchTerm));
        } catch (RestClientException | IllegalArgumentException ex) {
            model.addAttribute(
                    "apiError",
                    "The external news service is temporarily unavailable. Please try again shortly.");
        }

        return "research";
    }
}
