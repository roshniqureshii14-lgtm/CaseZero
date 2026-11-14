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
            @RequestParam(defaultValue = "missing persons") String q,
            Model model) {

        String searchTerm = q.trim();
        if (searchTerm.isBlank()) {
            searchTerm = "missing persons";
        }

        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("articles", List.of());

        try {
            model.addAttribute("articles", researchService.searchPakistanNews(searchTerm));
        } catch (Exception ex) {
            // Keep the page usable while recording the technical cause in the terminal.
            log.warn("Research Feed request failed: {}", ex.getMessage());
            model.addAttribute(
                    "apiError",
                    "The news service is not responding with usable results right now. "
                    + "Please try again in a little while.");
        }

        return "research";
    }
}
